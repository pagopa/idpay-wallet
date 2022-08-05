package it.gov.pagopa.wallet.service;

import it.gov.pagopa.wallet.dto.EnrollmentStatusDTO;
import it.gov.pagopa.wallet.dto.EvaluationDTO;
import it.gov.pagopa.wallet.dto.InitiativeDTO;
import it.gov.pagopa.wallet.dto.InitiativeListDTO;

public interface WalletService {
  void checkInitiative(String initiativeId);
  EnrollmentStatusDTO getEnrollmentStatus(String initiativeId, String userId);
  InitiativeDTO getWalletDetail(String initiativeId, String userId);
  void enrollInstrument(String initiativeId, String userId, String hpan);
  void enrollIban(String initiativeId, String userId, String iban, String description);
  InitiativeListDTO getInitiativeList(String userId);
  void createWallet(EvaluationDTO evaluationDTO);
  void updateEmail(String initiativeId, String userId, String email);

}
