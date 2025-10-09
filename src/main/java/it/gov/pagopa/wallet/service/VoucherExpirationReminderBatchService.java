package it.gov.pagopa.wallet.service;

public interface VoucherExpirationReminderBatchService {

    void runBatchManually(String initiativeId, int daysNumber);

}
