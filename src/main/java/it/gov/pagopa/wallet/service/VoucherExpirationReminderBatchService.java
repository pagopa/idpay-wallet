package it.gov.pagopa.wallet.service;

public interface VoucherExpirationReminderBatchService {

    void runReminderBatch(String initiativeId, int expiringDay);

}
