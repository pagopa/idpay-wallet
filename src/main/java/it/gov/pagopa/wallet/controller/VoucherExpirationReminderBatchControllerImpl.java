package it.gov.pagopa.wallet.controller;

import it.gov.pagopa.wallet.service.VoucherExpirationReminderBatchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VoucherExpirationReminderBatchControllerImpl implements VoucherExpirationReminderBatchController{

        private final VoucherExpirationReminderBatchService batchService;

        public VoucherExpirationReminderBatchControllerImpl(VoucherExpirationReminderBatchService batchService) {
            this.batchService = batchService;
        }


    @Override
        public ResponseEntity<Void> runBatchManually(String initiativeId, int daysNumber) {
            batchService.runBatchManually(initiativeId, daysNumber);
            return new ResponseEntity<>(HttpStatus.OK);
        }


}
