package it.gov.pagopa.wallet.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/idpay/wallet")
public interface VoucherExpirationReminderBatchController {

    @PostMapping("/batch/run/{initiativeId}")
    ResponseEntity<Void> runReminderBatch(
    @PathVariable("initiativeId") String initiativeId
            );




}
