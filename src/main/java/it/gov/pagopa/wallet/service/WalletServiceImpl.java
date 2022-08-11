package it.gov.pagopa.wallet.service;

import feign.FeignException;
import it.gov.pagopa.wallet.connector.PaymentInstrumentRestConnector;
import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.EnrollmentStatusDTO;
import it.gov.pagopa.wallet.dto.EvaluationDTO;
import it.gov.pagopa.wallet.dto.IbanQueueDTO;
import it.gov.pagopa.wallet.dto.IbanQueueWalletDTO;
import it.gov.pagopa.wallet.dto.InitiativeDTO;
import it.gov.pagopa.wallet.dto.InitiativeListDTO;
import it.gov.pagopa.wallet.dto.InstrumentCallBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentResponseDTO;
import it.gov.pagopa.wallet.dto.QueueOperationDTO;
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

  @Autowired
  WalletRepository walletRepository;

  @Autowired
  PaymentInstrumentRestConnector paymentInstrumentRestConnector;
  @Autowired
  IbanProducer ibanProducer;
  @Autowired
  TimelineProducer timelineProducer;
  @Autowired
  RTDProducer rtdProducer;
  @Autowired
  WalletMapper walletMapper;

  @Override
  public void checkInitiative(String initiativeId) {
    if (initiativeId.length() < 5) {
      throw new WalletException(HttpStatus.FORBIDDEN.value(), WalletConstants.ERROR_INITIATIVE_KO);
    }
  }

  @Override
  public EnrollmentStatusDTO getEnrollmentStatus(String initiativeId, String userId) {
    Wallet wallet =
        walletRepository
            .findByInitiativeIdAndUserId(initiativeId, userId)
            .orElseThrow(
                () ->
                    new WalletException(
                        HttpStatus.NOT_FOUND.value(), WalletConstants.ERROR_WALLET_NOT_FOUND));
    return new EnrollmentStatusDTO(wallet.getStatus());
  }

  @Override
  public InitiativeDTO getWalletDetail(String initiativeId, String userId) {
    Optional<Wallet> wallet = walletRepository.findByInitiativeIdAndUserId(initiativeId, userId);
    return wallet.map(this::walletToDto).orElseThrow(
        () ->
            new WalletException(
                HttpStatus.NOT_FOUND.value(), WalletConstants.ERROR_WALLET_NOT_FOUND));
  }

  @Override
  public void enrollInstrument(String initiativeId, String userId, String hpan) {
    Wallet wallet = walletRepository
        .findByInitiativeIdAndUserId(initiativeId, userId)
        .orElseThrow(
            () ->
                new WalletException(
                    HttpStatus.NOT_FOUND.value(), WalletConstants.ERROR_WALLET_NOT_FOUND));

    InstrumentCallBodyDTO dto = new InstrumentCallBodyDTO(
        userId,
        initiativeId,
        hpan,
        WalletConstants.CHANNEL_APP_IO,
        LocalDateTime.now());

    InstrumentResponseDTO responseDTO;
    try {
      responseDTO = paymentInstrumentRestConnector.enrollInstrument(dto);
    } catch (FeignException e) {
      throw new WalletException(e.status(), e.getMessage());
    }

    wallet.setNInstr(responseDTO.getNinstr());

    String newStatus =
        switch (wallet.getStatus()) {
          case WalletConstants.STATUS_NOT_REFUNDABLE:
            yield WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_INSTRUMENT;
          case WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_IBAN:
            yield WalletConstants.STATUS_REFUNDABLE;
          default:
            yield wallet.getStatus();
        };

    wallet.setStatus(newStatus);

    walletRepository.save(wallet);
    QueueOperationDTO queueOperationDTO = QueueOperationDTO.builder()
        .initiativeId(dto.getInitiativeId())
        .userId(dto.getUserId())
        .channel(dto.getChannel())
        .hpan(dto.getHpan())
        .operationType("ADD_INSTRUMENT")
        .operationDate(LocalDateTime.now())
        .build();
    timelineProducer.sendEvent(queueOperationDTO);
    QueueOperationDTO queueOperationDTOToRTD = QueueOperationDTO.builder()
        .hpan(dto.getHpan())
        .operationType("ADD_INSTRUMENT")
        .application("IDPAY")
        .operationDate(LocalDateTime.now())
        .build();
    rtdProducer.sendInstrument(queueOperationDTOToRTD);
  }

  @Override
  public void enrollIban(String initiativeId, String userId, String iban, String description) {
    Wallet wallet = walletRepository
        .findByInitiativeIdAndUserId(initiativeId, userId)
        .orElseThrow(
            () ->
                new WalletException(
                    HttpStatus.NOT_FOUND.value(), WalletConstants.ERROR_WALLET_NOT_FOUND));

    iban = iban.toUpperCase();
    this.formalControl(iban);
    if (wallet.getIban() == null || !(wallet.getIban().equals(iban))) {
      wallet.setIban(iban);
      IbanQueueDTO ibanQueueDTO = new IbanQueueDTO(wallet.getUserId(), wallet.getIban(),
          description, WalletConstants.CHANNEL_APP_IO, LocalDateTime.now());
      ibanProducer.sendIban(ibanQueueDTO);

    }

    String newStatus =
        switch (wallet.getStatus()) {
          case WalletConstants.STATUS_NOT_REFUNDABLE:
            yield WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_IBAN;
          case WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_INSTRUMENT:
            yield WalletConstants.STATUS_REFUNDABLE;
          default:
            yield wallet.getStatus();
        };

    wallet.setStatus(newStatus);

    walletRepository.save(wallet);

    QueueOperationDTO queueOperationDTO = QueueOperationDTO.builder()
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

      QueueOperationDTO dto = QueueOperationDTO.builder()
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
    Wallet wallet =
        walletRepository
            .findByInitiativeIdAndUserId(initiativeId, userId)
            .orElseThrow(
                () ->
                    new WalletException(
                        HttpStatus.NOT_FOUND.value(), WalletConstants.ERROR_WALLET_NOT_FOUND));

    if (wallet.getEmail() == null || !wallet.getEmail().equals(email)) {

      wallet.setEmail(email);
      wallet.setEmailUpdate(LocalDateTime.now());
      setStatus(wallet);
      walletRepository.save(wallet);

      QueueOperationDTO event =
          QueueOperationDTO.builder()
              .initiativeId(initiativeId)
              .userId(userId)
              .email(email)
              .operationDate(wallet.getEmailUpdate())
              .operationType("ADD_EMAIL")
              .build();

      timelineProducer.sendEvent(event);
    }
  }

  private void setStatus(Wallet wallet) {
    boolean hasIban = wallet.getIban() != null;
    boolean hasInstrument = wallet.getNInstr() > 0;
    boolean hasEmail = wallet.getEmail() != null;
    String status = WalletStatus.getByBooleans(hasIban, hasInstrument, hasEmail).name();
    wallet.setStatus(status);
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
    String newStatus =
        switch (wallet.getStatus()) {
          case WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_IBAN:
            yield WalletConstants.STATUS_NOT_REFUNDABLE;
          case WalletConstants.STATUS_REFUNDABLE:
            yield WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_INSTRUMENT;
          default:
            yield wallet.getStatus();
        };
    wallet.setStatus(newStatus);

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
