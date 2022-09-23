package it.gov.pagopa.wallet.service;

import it.gov.pagopa.wallet.dto.EnrollmentStatusDTO;
import it.gov.pagopa.wallet.dto.EvaluationDTO;
import it.gov.pagopa.wallet.dto.IbanQueueWalletDTO;
import it.gov.pagopa.wallet.dto.WalletDTO;
import it.gov.pagopa.wallet.dto.InitiativeListDTO;
import it.gov.pagopa.wallet.dto.RewardTransactionDTO;

public interface WalletService {
  EnrollmentStatusDTO getEnrollmentStatus(String initiativeId, String userId);
  WalletDTO getWalletDetail(String initiativeId, String userId);
  void enrollInstrument(String initiativeId, String userId, String hpan);
  void deleteInstrument(String initiativeId, String userId, String hpan);
  void enrollIban(String initiativeId, String userId, String iban, String description);
  InitiativeListDTO getInitiativeList(String userId);
  void createWallet(EvaluationDTO evaluationDTO);
  void deleteOperation(IbanQueueWalletDTO ibanQueueWalletDTO);
  void unsubscribe(String initiativeId, String userId);
  void processTransaction(RewardTransactionDTO rewardTransactionDTO);
}
