package it.gov.pagopa.wallet.controller;

import it.gov.pagopa.wallet.dto.EnrollmentStatusDTO;
import it.gov.pagopa.wallet.dto.IbanBodyDTO;
import it.gov.pagopa.wallet.dto.InitiativeListDTO;
import it.gov.pagopa.wallet.dto.InstrumentAckDTO;
import it.gov.pagopa.wallet.dto.InstrumentIssuerDTO;
import it.gov.pagopa.wallet.dto.WalletDTO;
import it.gov.pagopa.wallet.dto.WalletPIBodyDTO;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/idpay/wallet")
public interface WalletController {

  @PutMapping("/{initiativeId}/{userId}/instruments/{idWallet}")
  ResponseEntity<Void> enrollInstrument(
      @PathVariable("initiativeId") String initiativeId,
      @PathVariable("userId") String userId,
      @PathVariable("idWallet") String idWallet);

  @DeleteMapping("/{initiativeId}/{userId}/instruments/{instrumentId}")
  ResponseEntity<Void> deleteInstrument(
      @PathVariable("initiativeId") String initiativeId,
      @PathVariable("userId") String userId,
      @PathVariable("instrumentId") String instrumentId);

  @GetMapping("/{initiativeId}/{userId}/status")
  ResponseEntity<EnrollmentStatusDTO> enrollmentStatus(
      @PathVariable("initiativeId") String initiativeId, @PathVariable("userId") String userId);

  @GetMapping("/{initiativeId}/{userId}")
  ResponseEntity<WalletDTO> walletDetail(
      @PathVariable("initiativeId") String initiativeId, @PathVariable("userId") String userId);

  @GetMapping("/initiative/{initiativeId}/{userId}")
  ResponseEntity<WalletDTO> walletIssuerDetail(
      @PathVariable("initiativeId") String initiativeId, @PathVariable("userId") String userId);

  @PutMapping("/updateWallet")
  ResponseEntity<Void> updateWallet(
      @Valid @RequestBody WalletPIBodyDTO body);

  @PutMapping("/{initiativeId}/{userId}/iban")
  ResponseEntity<Void> enrollIban(
      @Valid @RequestBody IbanBodyDTO body,
      @PathVariable("initiativeId") String initiativeId,
      @PathVariable("userId") String userId);

  @GetMapping("/{userId}")
  ResponseEntity<InitiativeListDTO> initiativeList(@PathVariable("userId") String userId);

  @DeleteMapping("/{initiativeId}/{userId}/unsubscribe")
  ResponseEntity<Void> unsubscribeInitiative(
      @PathVariable("initiativeId") String initiativeId, @PathVariable("userId") String userId);

  @PutMapping("/acknowledge")
  ResponseEntity<Void> processAck(@Valid @RequestBody InstrumentAckDTO body);

  @PutMapping("/{initiativeId}/{userId}/instruments")
  ResponseEntity<Void> enrollInstrumentIssuer(
      @Valid @RequestBody InstrumentIssuerDTO body,
      @PathVariable("initiativeId") String initiativeId,
      @PathVariable("userId") String userId);
}
