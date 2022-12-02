package it.gov.pagopa.wallet.controller;

import it.gov.pagopa.wallet.dto.EnrollmentStatusDTO;
import it.gov.pagopa.wallet.dto.IbanBodyDTO;
import it.gov.pagopa.wallet.dto.InitiativeListDTO;
import it.gov.pagopa.wallet.dto.InstrumentAckDTO;
import it.gov.pagopa.wallet.dto.WalletDTO;
import it.gov.pagopa.wallet.dto.WalletPIBodyDTO;
import it.gov.pagopa.wallet.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WalletControllerImpl implements WalletController {

  @Autowired WalletService walletService;

  @Override
  public ResponseEntity<Void> enrollInstrument(
      String initiativeId, String userId, String idWallet) {
    walletService.enrollInstrument(initiativeId, userId, idWallet);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Void> deleteInstrument(String initiativeId, String userId, String instrumentId) {
    walletService.deleteInstrument(initiativeId, userId, instrumentId);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Override
  public ResponseEntity<EnrollmentStatusDTO> enrollmentStatus(String initiativeId, String userId) {
    EnrollmentStatusDTO enrollmentStatusDTO =
        walletService.getEnrollmentStatus(initiativeId, userId);
    return new ResponseEntity<>(enrollmentStatusDTO, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<WalletDTO> walletDetail(String initiativeId, String userId) {
    WalletDTO walletDTO = walletService.getWalletDetail(initiativeId, userId);
    return new ResponseEntity<>(walletDTO, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Void> updateWallet(WalletPIBodyDTO body) {
    walletService.updateWallet(body);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<Void> enrollIban(IbanBodyDTO body, String initiativeId, String userId) {
    walletService.enrollIban(initiativeId, userId, body.getIban(), body.getChannel(), body.getDescription());
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Override
  public ResponseEntity<InitiativeListDTO> initiativeList(String userId) {
    InitiativeListDTO initiativeDTO = walletService.getInitiativeList(userId);
    return new ResponseEntity<>(initiativeDTO, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Void> unsubscribeInitiative(String initiativeId, String userId) {
    walletService.unsubscribe(initiativeId, userId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<Void> processAck(InstrumentAckDTO body) {
    walletService.processAck(body);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
