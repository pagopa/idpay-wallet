package it.gov.pagopa.wallet.controller;

import it.gov.pagopa.wallet.dto.*;

import jakarta.validation.Valid;
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

  @PutMapping("/{initiativeId}/{userId}/suspend")
  ResponseEntity<Void> suspendWallet(
          @PathVariable("initiativeId") String initiativeId,
          @PathVariable("userId") String userId);

  @PutMapping("/{initiativeId}/{userId}/readmit")
  ResponseEntity<Void> readmitWallet(
          @PathVariable("initiativeId") String initiativeId,
          @PathVariable("userId") String userId);

  @GetMapping("/{userId}")
  ResponseEntity<InitiativeListDTO> initiativeList(@PathVariable("userId") String userId);

  @DeleteMapping("/{initiativeId}/{userId}/unsubscribe")
  ResponseEntity<Void> unsubscribeInitiative(
      @PathVariable("initiativeId") String initiativeId, @PathVariable("userId") String userId);

  @PutMapping("/acknowledge")
  ResponseEntity<Void> processAck(@RequestBody InstrumentAckDTO body);

  @PutMapping("/{initiativeId}/{userId}/instruments")
  ResponseEntity<Void> enrollInstrumentIssuer(
      @Valid @RequestBody InstrumentIssuerDTO body,
      @PathVariable("initiativeId") String initiativeId,
      @PathVariable("userId") String userId);

  @GetMapping("/instrument/{idWallet}/{userId}/initiatives")
  ResponseEntity<InitiativesWithInstrumentDTO> getInitiativesWithInstrument(@PathVariable("idWallet") String idWallet,
                                                                            @PathVariable("userId") String userId);
  @PutMapping("/{initiativeId}/{userId}/code/instruments")
  ResponseEntity<Void> enrollInstrumentCode(
          @PathVariable("initiativeId") String initiativeId,
          @PathVariable("userId") String userId,
          @Valid @RequestBody EnrollCodeDTO body);
}
