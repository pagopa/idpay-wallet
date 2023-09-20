package it.gov.pagopa.wallet.repository;

import it.gov.pagopa.wallet.model.Wallet;
import it.gov.pagopa.wallet.model.Wallet.RefundHistory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface WalletUpdatesRepository {
  void deleteIban(String initiativeId, String userId, String status);
  void enrollIban(String initiativeId, String userId, String iban, String status);
  void suspendWallet(String initiativeId, String userId, String status, LocalDateTime localDateTime);
  void readmitWallet(String initiativeId, String userId, String status, LocalDateTime localDateTime);
  Wallet rewardTransaction(String initiativeId, String userId, LocalDateTime trxElaborationTimestamp, BigDecimal amount, BigDecimal accrued);
  boolean rewardFamilyTransaction(String initiativeId, String familyId, LocalDateTime trxElaborationTimestamp, BigDecimal amount);
  void processRefund(String initiativeId, String userId, BigDecimal refunded, Map<String, RefundHistory> history);
  void updateInstrumentNumber(String initiativeId, String userId, int nInstr, String status);
  void decreaseInstrumentNumber(String initiativeId, String userId, String status);
  List<Wallet> deletePaged(String initiativeId, int pageSize);
}
