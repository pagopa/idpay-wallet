package it.gov.pagopa.wallet.controller;

import it.gov.pagopa.wallet.dto.EnrollmentStatusDTO;
import it.gov.pagopa.wallet.dto.IbanBodyDTO;
import it.gov.pagopa.wallet.dto.InitiativeDTO;
import it.gov.pagopa.wallet.dto.InitiativeListDTO;
import it.gov.pagopa.wallet.dto.InstrumentBodyDTO;
import it.gov.pagopa.wallet.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WalletControllerImpl implements WalletController {

  @Autowired
  WalletService walletService;

  @Override
  public ResponseEntity<Void> enrollInstrument(InstrumentBodyDTO body, String userId) {
    walletService.checkInitiative(body.getInitiativeId());
    walletService.enrollInstrument(body.getInitiativeId(), userId, body.getHpan());
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Override
  public ResponseEntity<EnrollmentStatusDTO> enrollmentStatus(String initiativeId, String userId) {
    EnrollmentStatusDTO enrollmentStatusDTO =
        walletService.getEnrollmentStatus(initiativeId, userId);
    return new ResponseEntity<>(enrollmentStatusDTO, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<InitiativeDTO> walletDetail(String initiativeId, String userId) {
    InitiativeDTO initiativeDTO = walletService.getWalletDetail(initiativeId, userId);
    return new ResponseEntity<>(initiativeDTO, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Void> enrollIban(IbanBodyDTO body, String userId) {
    walletService.checkInitiative(body.getInitiativeId());
    walletService.enrollIban(body.getInitiativeId(), userId, body.getIban(), body.getDescription());
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Override
  public ResponseEntity<InitiativeListDTO> initiativeList(String userId) {
    InitiativeListDTO initiativeDTO = walletService.getInitiativeList(userId);
    return new ResponseEntity<>(initiativeDTO, HttpStatus.OK);
  }
}
