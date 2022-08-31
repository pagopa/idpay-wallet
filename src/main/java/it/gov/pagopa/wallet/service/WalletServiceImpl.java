package it.gov.pagopa.wallet.service;

import feign.FeignException;
import it.gov.pagopa.wallet.connector.OnboardingRestConnector;
import it.gov.pagopa.wallet.connector.PaymentInstrumentRestConnector;
import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.EmailDTO;
import it.gov.pagopa.wallet.dto.EnrollmentStatusDTO;
import it.gov.pagopa.wallet.dto.EvaluationDTO;
import it.gov.pagopa.wallet.dto.IbanQueueDTO;
import it.gov.pagopa.wallet.dto.IbanQueueWalletDTO;
import it.gov.pagopa.wallet.dto.InitiativeDTO;
import it.gov.pagopa.wallet.dto.InitiativeListDTO;
import it.gov.pagopa.wallet.dto.InstrumentCallBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentResponseDTO;
import it.gov.pagopa.wallet.dto.QueueOperationDTO;
import it.gov.pagopa.wallet.dto.UnsubscribeCallDTO;
import it.gov.pagopa.wallet.dto.mapper.WalletMapper;
import it.gov.pagopa.wallet.enums.WalletStatus;
import it.gov.pagopa.wallet.event.IbanProducer;
import it.gov.pagopa.wallet.event.RTDProducer;
import it.gov.pagopa.wallet.event.TimelineProducer;
import it.gov.pagopa.wallet.exception.WalletException;
import it.gov.pagopa.wallet.model.Wallet;
import it.gov.pagopa.wallet.repository.WalletRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.iban4j.IbanUtil;
import org.iban4j.UnsupportedCountryException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WalletServiceImpl implements WalletService {

  @Autowired WalletRepository walletRepository;

  @Autowired PaymentInstrumentRestConnector paymentInstrumentRestConnector;

  @Autowired
  OnboardingRestConnector onboardingRestConnector;
  @Autowired IbanProducer ibanProducer;
  @Autowired TimelineProducer timelineProducer;
  @Autowired RTDProducer rtdProducer;
  @Autowired WalletMapper walletMapper;

  @Override
  public void checkInitiative(String initiativeId) {
    if (initiativeId.length() < 5) {
      throw new WalletException(HttpStatus.FORBIDDEN.value(), WalletConstants.ERROR_INITIATIVE_KO);
    }
  }

  @Override
  public EnrollmentStatusDTO getEnrollmentStatus(String initiativeId, String userId) {
    Wallet wallet = findByInitiativeIdAndUserId(initiativeId, userId);
    return new EnrollmentStatusDTO(wallet.getStatus());
  }

  @Override
  public InitiativeDTO getWalletDetail(String initiativeId, String userId) {
    Optional<Wallet> wallet = walletRepository.findByInitiativeIdAndUserId(initiativeId, userId);
    return wallet
        .map(this::walletToDto)
        .orElseThrow(
            () ->
                new WalletException(
                    HttpStatus.NOT_FOUND.value(), WalletConstants.ERROR_WALLET_NOT_FOUND));
  }

  @Override
  public void enrollInstrument(String initiativeId, String userId, String hpan) {
    Wallet wallet = findByInitiativeIdAndUserId(initiativeId, userId);

    if(this.getEnrollmentStatus(initiativeId,userId).getStatus().equals(WalletStatus.UNSUBSCRIBED)){
      throw new WalletException(HttpStatus.BAD_REQUEST.value(), WalletConstants.ERROR_INITIATIVE_UNSUBSCRIBED);
    }

    InstrumentCallBodyDTO dto =
        new InstrumentCallBodyDTO(
            userId, initiativeId, hpan, WalletConstants.CHANNEL_APP_IO, LocalDateTime.now());

    InstrumentResponseDTO responseDTO;
    try {
      responseDTO = paymentInstrumentRestConnector.enrollInstrument(dto);
    } catch (FeignException e) {
      throw new WalletException(e.status(), e.getMessage());
    }

    wallet.setNInstr(responseDTO.getNinstr());

    setStatus(wallet);

    walletRepository.save(wallet);
    QueueOperationDTO queueOperationDTO =
        QueueOperationDTO.builder()
            .initiativeId(dto.getInitiativeId())
            .userId(dto.getUserId())
            .channel(dto.getChannel())
            .hpan(dto.getHpan())
            .operationType("ADD_INSTRUMENT")
            .operationDate(LocalDateTime.now())
            .build();
    timelineProducer.sendEvent(queueOperationDTO);
    QueueOperationDTO queueOperationDTOToRTD =
        QueueOperationDTO.builder()
            .hpan(dto.getHpan())
            .operationType("ADD_INSTRUMENT")
            .application("IDPAY")
            .operationDate(LocalDateTime.now())
            .build();
    rtdProducer.sendInstrument(queueOperationDTOToRTD);
  }

  @Override
  public void enrollIban(String initiativeId, String userId, String iban, String description) {
    Wallet wallet = findByInitiativeIdAndUserId(initiativeId, userId);
    if(this.getEnrollmentStatus(initiativeId,userId).getStatus().equals(WalletStatus.UNSUBSCRIBED)){
      throw new WalletException(HttpStatus.BAD_REQUEST.value(), WalletConstants.ERROR_INITIATIVE_UNSUBSCRIBED);
    }

    iban = iban.toUpperCase();
    this.formalControl(iban);
    if (wallet.getIban() == null || !(wallet.getIban().equals(iban))) {
      wallet.setIban(iban);
      IbanQueueDTO ibanQueueDTO = new IbanQueueDTO(wallet.getUserId(), wallet.getIban(),
          description, WalletConstants.CHANNEL_APP_IO, LocalDateTime.now());
      ibanProducer.sendIban(ibanQueueDTO);
    }

    setStatus(wallet);

    walletRepository.save(wallet);

    QueueOperationDTO queueOperationDTO =
        QueueOperationDTO.builder()
            .initiativeId(wallet.getInitiativeId())
            .userId(wallet.getUserId())
            .channel(WalletConstants.CHANNEL_APP_IO)
            .iban(wallet.getIban())
            .operationType("ADD_IBAN")
            .operationDate(LocalDateTime.now())
            .build();
    timelineProducer.sendEvent(queueOperationDTO);
  }

  @Override
  public InitiativeListDTO getInitiativeList(String userId) {
    List<Wallet> walletList = walletRepository.findByUserId(userId);
    InitiativeListDTO initiativeListDTO = new InitiativeListDTO();
    List<InitiativeDTO> initiativeDTOList = new ArrayList<>();

    for (Wallet wallet : walletList) {
      initiativeDTOList.add(walletToDto(wallet));
    }
    initiativeListDTO.setInitiativeList(initiativeDTOList);
    return initiativeListDTO;
  }

  @Override
  public void createWallet(EvaluationDTO evaluationDTO) {
    if (evaluationDTO.getStatus().equals(WalletConstants.STATUS_ONBOARDING_OK)) {
      Wallet wallet = walletMapper.map(evaluationDTO);
      walletRepository.save(wallet);

      QueueOperationDTO dto =
          QueueOperationDTO.builder()
              .initiativeId(evaluationDTO.getInitiativeId())
              .userId(evaluationDTO.getUserId())
              .operationType(WalletConstants.ONBOARDING_OPERATION)
              .operationDate(LocalDateTime.now())
              .build();

      timelineProducer.sendEvent(dto);
    }
  }

  @Override
  public void updateEmail(String initiativeId, String userId, String email) {
    Wallet wallet = findByInitiativeIdAndUserId(initiativeId, userId);

    if (wallet.getEmail() == null || !wallet.getEmail().equals(email)) {

      wallet.setEmail(email);
      wallet.setEmailUpdateDate(LocalDateTime.now());
      setStatus(wallet);
      walletRepository.save(wallet);

      QueueOperationDTO event =
          QueueOperationDTO.builder()
              .initiativeId(initiativeId)
              .userId(userId)
              .email(email)
              .operationDate(wallet.getEmailUpdateDate())
              .operationType("ADD_EMAIL")
              .build();

      timelineProducer.sendEvent(event);
    }
  }

  @Override
  public EmailDTO getEmail(String initiativeId, String userId) {
    Wallet wallet = findByInitiativeIdAndUserId(initiativeId, userId);
    return new EmailDTO(wallet.getEmail(), wallet.getEmailUpdateDate().toString());
  }

  @Override
  public void unsubscribe(String initiativeId, String userId) {
    Wallet wallet = findByInitiativeIdAndUserId(initiativeId, userId);
    if(!wallet.getStatus().equals(WalletStatus.UNSUBSCRIBED)) {
      wallet.setStatus(WalletStatus.UNSUBSCRIBED);
      wallet.setUnsubscribeDate(LocalDateTime.now());
      UnsubscribeCallDTO unsubscribeCallDTO = new UnsubscribeCallDTO(initiativeId, userId, wallet.getUnsubscribeDate().toString());
      try {
        paymentInstrumentRestConnector.disableAllInstrument(unsubscribeCallDTO);
        onboardingRestConnector.disableOnboarding(unsubscribeCallDTO);
        walletRepository.save(wallet);

      } catch (FeignException e) {
        throw new WalletException(e.status(), e.getMessage());
      }
    }
  }

  private Wallet findByInitiativeIdAndUserId(String initiativeId, String userId) {
    return walletRepository
        .findByInitiativeIdAndUserId(initiativeId, userId)
        .orElseThrow(
            () ->
                new WalletException(
                    HttpStatus.NOT_FOUND.value(), WalletConstants.ERROR_WALLET_NOT_FOUND));
  }

  private void setStatus(Wallet wallet) {
    if(!wallet.getStatus().equals(WalletStatus.UNSUBSCRIBED)) {
      boolean hasIban = wallet.getIban() != null;
      boolean hasInstrument = wallet.getNInstr() > 0;
      boolean hasEmail = wallet.getEmail() != null;
      String status = WalletStatus.getByBooleans(hasIban, hasInstrument, hasEmail).name();
      wallet.setStatus(status);
    }
  }

  @Override
  public void deleteOperation(IbanQueueWalletDTO iban) {
    Wallet wallet = walletRepository.findByUserIdAndIban(iban.getUserId(), iban.getIban())
        .orElseThrow(
        () ->
            new WalletException(
                HttpStatus.NOT_FOUND.value(), WalletConstants.ERROR_WALLET_NOT_FOUND));
    log.debug("Entry consumer: " + wallet.toString());

    wallet.setIban(null);
    setStatus(wallet);

    walletRepository.save(wallet);
    log.debug("Finished consumer: " +wallet.toString());
  }

  private InitiativeDTO walletToDto(Wallet wallet) {
    ModelMapper modelmapper = new ModelMapper();
    return modelmapper.map(wallet, InitiativeDTO.class);
  }

  private void formalControl(String iban) {
    Iban ibanValidator = Iban.valueOf(iban);
    IbanUtil.validate(iban);
    if (!ibanValidator.getCountryCode().equals(CountryCode.IT)) {
      throw new UnsupportedCountryException(iban + " Iban is not italian");
    }
  }
}
