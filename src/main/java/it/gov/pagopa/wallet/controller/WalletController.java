package it.gov.pagopa.wallet.controller;

import it.gov.pagopa.wallet.dto.EnrollmentStatusDTO;
import it.gov.pagopa.wallet.dto.IbanBodyDTO;
import it.gov.pagopa.wallet.dto.IbanDTO;
import it.gov.pagopa.wallet.dto.InitiativeListDTO;
import it.gov.pagopa.wallet.dto.InitiativeDTO;
import it.gov.pagopa.wallet.dto.InstrumentBodyDTO;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
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
   * @param body
   * @return
   */
  @PutMapping("/instrument/{userId}")
  ResponseEntity<Void> enrollInstrument(@Valid @RequestBody InstrumentBodyDTO body, @PathVariable String userId);

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
  ResponseEntity<InitiativeDTO> walletDetail(
      @PathVariable("initiativeId") String initiativeId, @PathVariable("userId") String userId);

  /**
   * Enrollment of a Iban
   *
   * @param userId
   * @param body
   * @return
   */
  @PutMapping("/iban/{userId}")
  ResponseEntity<Void> enrollIban(@Valid @RequestBody IbanBodyDTO body, @PathVariable String userId);

  /**
   * Get IbanDTO
   *
   * @param userId
   * @param initiativeId
   * @return
   */
  @GetMapping("/{userId}/{initiativeId}/iban")
  ResponseEntity<IbanDTO> getIban(@PathVariable("userId") String userId, @PathVariable("initiativeId") String initiativeId);

  /**
   * Returns the active initiative lists
   *
   * @param userId
   * @return
   */
  @GetMapping("/initiative/{userId}")
  ResponseEntity<InitiativeListDTO> initiativeList(@Valid @PathVariable("userId") String userId);

}
