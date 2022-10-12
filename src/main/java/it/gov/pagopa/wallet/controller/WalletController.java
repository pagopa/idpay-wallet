package it.gov.pagopa.wallet.controller;

import it.gov.pagopa.wallet.dto.EnrollmentStatusDTO;
import it.gov.pagopa.wallet.dto.IbanBodyDTO;
import it.gov.pagopa.wallet.dto.WalletDTO;
import it.gov.pagopa.wallet.dto.InitiativeListDTO;
import it.gov.pagopa.wallet.dto.InstrumentBodyDTO;
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

  /**
   * Enrollment of a Payment Instrument
   *
   * @param userId
   * @param initiativeId
   * @param body
   * @return
   */
  @PutMapping("/{initiativeId}/{userId}/instruments")
  ResponseEntity<Void> enrollInstrument(
      @Valid @RequestBody InstrumentBodyDTO body,
      @PathVariable("initiativeId") String initiativeId,
      @PathVariable("userId") String userId);

  /**
   * Deactivation of a Payment Instrument
   *
   * @param initiativeId
   * @param userId
   * @param hpan
   * @return
   */
  @DeleteMapping("/{initiativeId}/{userId}/instruments/{hpan}")
  ResponseEntity<Void> deleteInstrument(
      @PathVariable("initiativeId") String initiativeId,
      @PathVariable("userId") String userId,
      @PathVariable("hpan") String hpan);

  /**
   * Returns the actual enrollment status
   *
   * @param initiativeId
   * @param userId
   * @return
   */
  @GetMapping("/{initiativeId}/{userId}/status")
  ResponseEntity<EnrollmentStatusDTO> enrollmentStatus(
      @PathVariable("initiativeId") String initiativeId, @PathVariable("userId") String userId);

  /**
   * Returns the detail of an active initiative for a citizen
   *
   * @param initiativeId
   * @param userId
   * @return
   */
  @GetMapping("/{initiativeId}/{userId}")
  ResponseEntity<WalletDTO> walletDetail(
      @PathVariable("initiativeId") String initiativeId, @PathVariable("userId") String userId);

  /**
   * Enrollment of a Iban
   *
   * @param userId
   * @param body
   * @return
   */
  @PutMapping("/{initiativeId}/{userId}/iban")
  ResponseEntity<Void> enrollIban(
      @Valid @RequestBody IbanBodyDTO body,
      @PathVariable("initiativeId") String initiativeId,
      @PathVariable("userId") String userId);

  /**
   * Returns the active initiative lists
   *
   * @param userId
   * @return
   */
  @GetMapping("/{userId}")
  ResponseEntity<InitiativeListDTO> initiativeList(@PathVariable("userId") String userId);

  /**
   * unsubscrive intiative
   *
   * @param initiativeId
   * @param userId
   * @return
   */
  @DeleteMapping("/{initiativeId}/{userId}/unsubscribe")
  ResponseEntity<Void> unsubscribeInitiative(
      @PathVariable("initiativeId") String initiativeId, @PathVariable("userId") String userId);
}
