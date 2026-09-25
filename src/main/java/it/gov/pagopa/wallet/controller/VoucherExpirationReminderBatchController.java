package it.gov.pagopa.wallet.controller;

import it.gov.pagopa.wallet.dto.ReminderBatchRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/idpay/wallet")
public interface VoucherExpirationReminderBatchController {

    @PostMapping("/batch/run")
    ResponseEntity<Void> runReminderBatch(
            @Valid @RequestBody ReminderBatchRequestDTO request
    );

    /**
     * @deprecated retained for backward compatibility during the multi-initiative
     * cronjob migration. Use {@link #runReminderBatch(ReminderBatchRequestDTO)} instead.
     */
    @Deprecated(since = "multi-initiative-migration")
    @PostMapping("/batch/run/{initiativeId}")
    ResponseEntity<Void> runReminderBatch(
            @PathVariable("initiativeId") String initiativeId
    );


}
