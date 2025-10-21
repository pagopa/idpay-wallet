package it.gov.pagopa.wallet.controller;

import it.gov.pagopa.wallet.service.VoucherExpirationReminderBatchService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VoucherExpirationReminderBatchControllerImpl implements VoucherExpirationReminderBatchController {

    private final VoucherExpirationReminderBatchService batchService;

    @Value("${app.wallet.expiringDay}")
    private int expiringDay;

    public VoucherExpirationReminderBatchControllerImpl(VoucherExpirationReminderBatchService batchService) {
        this.batchService = batchService;
    }


    @Override
    public ResponseEntity<Void> runReminderBatch(String initiativeId) {
        batchService.runReminderBatch(initiativeId, expiringDay);
        return new ResponseEntity<>(HttpStatus.OK);
    }


}
