package it.gov.pagopa.wallet.controller;

import it.gov.pagopa.wallet.dto.EnrollmentStatusDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/idpay/hb/wallet")
public interface WalletIssuerController {

  @GetMapping("/{initiativeId}/{userId}/status")
  ResponseEntity<EnrollmentStatusDTO> enrollmentStatus(
      @PathVariable("initiativeId") String initiativeId, @PathVariable("userId") String userId);

}
