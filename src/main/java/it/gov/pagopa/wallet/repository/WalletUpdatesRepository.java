package it.gov.pagopa.wallet.repository;

import it.gov.pagopa.wallet.model.Wallet;
import it.gov.pagopa.wallet.model.Wallet.RefundHistory;
import java.math.BigDecimal;
import java.util.Map;

public interface WalletUpdatesRepository {
  Wallet deleteIban(String initiativeId, String userId, String iban);
  void enrollIban(String initiativeId, String userId, String iban, String status);
  Wallet rewardTransaction(String initiativeId, String userId, BigDecimal amount, BigDecimal accrued, Long nTrx);
  void processRefund(String initiativeId, String userId, BigDecimal refunded, Map<String, RefundHistory> history);
  void updateInstrumentNumber(String initiativeId, String userId, int nInstr, String status);
  void decreaseInstrumentNumber(String initiativeId, String userId, String status);
  void setStatus(String initiativeId, String userId, String status);
}
