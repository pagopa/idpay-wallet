package it.gov.pagopa.wallet.service;

import it.gov.pagopa.wallet.dto.*;

public interface WalletService {
  EnrollmentStatusDTO getEnrollmentStatus(String initiativeId, String userId);
  WalletDTO getWalletDetail(String initiativeId, String userId);
  WalletDTO getWalletDetailIssuer(String initiativeId, String userId);
  void enrollInstrument(String initiativeId, String userId, String idWallet);
  void deleteInstrument(String initiativeId, String userId, String instrumentId);
  void enrollIban(String initiativeId, String userId, String iban, String channel, String description);
  void suspendWallet(String initiativeId, String userId);
  InitiativeListDTO getInitiativeList(String userId);
  void createWallet(EvaluationDTO evaluationDTO);
  void deleteOperation(IbanQueueWalletDTO ibanQueueWalletDTO);
  void unsubscribe(String initiativeId, String userId);
  void processTransaction(RewardTransactionDTO rewardTransactionDTO);
  void updateWallet(WalletPIBodyDTO walletPIBodyDTO);
  void processAck(InstrumentAckDTO instrumentAckDTO);
  void processRefund(RefundDTO refundDTO);
  void enrollInstrumentIssuer(String initiativeId, String userId, InstrumentIssuerDTO body);
  InitiativesWithInstrumentDTO getInitiativesWithInstrument(String idWallet, String userId);
}
