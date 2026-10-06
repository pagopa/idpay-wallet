package it.gov.pagopa.wallet.service;

import java.util.List;

public interface VoucherExpirationReminderBatchService {

    void runReminderBatch(String initiativeId, int expiringDay);

    void runReminderBatch(List<String> initiativeIds, int expiringDay);
}
