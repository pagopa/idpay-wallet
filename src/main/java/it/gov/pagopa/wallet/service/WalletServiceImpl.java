package it.gov.pagopa.wallet.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.EnrollmentStatusDTO;
import it.gov.pagopa.wallet.dto.InitiativeDTO;
import it.gov.pagopa.wallet.dto.InitiativeListDTO;
import it.gov.pagopa.wallet.dto.InstrumentCallBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentResponseDTO;
import it.gov.pagopa.wallet.exception.WalletException;
import it.gov.pagopa.wallet.model.Wallet;
import it.gov.pagopa.wallet.repository.WalletRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.iban4j.IbanUtil;
import org.iban4j.UnsupportedCountryException;
import java.util.Optional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Service
public class WalletServiceImpl implements WalletService {

  @Autowired
  WalletRepository walletRepository;

  @Autowired
  WalletRestService walletRestService;

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
      responseDTO = walletRestService.callPaymentInstrument(dto);
    } catch (HttpClientErrorException e) {
      throw new WalletException(e.getRawStatusCode(), e.getMessage());
    } catch (JsonProcessingException jpe) {
      throw new WalletException(HttpStatus.BAD_REQUEST.value(), jpe.getMessage());
    }

    wallet.setNInstr(Objects.requireNonNull(responseDTO).getNinstr());

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
    if(wallet.getIban()==null || !(wallet.getIban().equals(iban))) {
      //pub su coda
      wallet.setIban(iban);
      wallet.setDescription(description);
      wallet.setChannel(WalletConstants.CHANNEL_APP_IO);
      wallet.setHolderBank(WalletConstants.HOLDER_BANK);
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
  
  private InitiativeDTO walletToDto(Wallet wallet){
    ModelMapper modelmapper = new ModelMapper();
    return wallet != null ? modelmapper.map(wallet, InitiativeDTO.class) : null;
  }
  
  private void formalControl(String iban){
    Iban ibanValidator = Iban.valueOf(iban);
    IbanUtil.validate(iban);
    if(!ibanValidator.getCountryCode().equals(CountryCode.IT)){
      throw new UnsupportedCountryException(iban+" Iban is not italian");
    }
  }
}
