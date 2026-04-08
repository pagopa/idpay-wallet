package it.gov.pagopa.wallet.repository;

import it.gov.pagopa.wallet.model.Wallet;
import it.gov.pagopa.wallet.model.Wallet.RefundHistory;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface WalletUpdatesRepository {
  void deleteIban(String initiativeId, String userId, String status);
  void enrollIban(String initiativeId, String userId, String iban, String status);
  void suspendWallet(String initiativeId, String userId, String status, Instant instant);
  void readmitWallet(String initiativeId, String userId, String status, Instant instant);
  Wallet rewardTransaction(String initiativeId, String userId, Instant trxElaborationTimestamp, Long amountCents, Long accruedCents, Long counterVersion);
  boolean rewardFamilyTransaction(String initiativeId, String familyId, Instant trxElaborationTimestamp, Long amountCents, Long counterVersion);
  void processRefund(String initiativeId, String userId, Long refundedCents, Map<String, RefundHistory> history);
  void updateInstrumentNumber(String initiativeId, String userId, int nInstr, String status);
  void decreaseInstrumentNumber(String initiativeId, String userId, String status);
  List<Wallet> deletePaged(String initiativeId, int pageSize);
  Wallet rewardFamilyUserTransaction(String initiativeId, String userId, Instant elaborationDateTime, List<Long> counterHistory, Long accruedCents);
}
