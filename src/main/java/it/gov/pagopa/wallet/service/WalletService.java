package it.gov.pagopa.wallet.service;

import it.gov.pagopa.wallet.dto.EnrollmentStatusDTO;
import it.gov.pagopa.wallet.dto.EvaluationDTO;
import it.gov.pagopa.wallet.dto.IbanDTO;
import it.gov.pagopa.wallet.dto.InitiativeListDTO;
import it.gov.pagopa.wallet.dto.InitiativeDTO;

public interface WalletService {
  void checkInitiative(String initiativeId);
  EnrollmentStatusDTO getEnrollmentStatus(String initiativeId, String userId);
  InitiativeDTO getWalletDetail(String initiativeId, String userId);
  void enrollInstrument(String initiativeId, String userId, String hpan);
  void enrollIban(String initiativeId, String userId, String iban, String description);
  IbanDTO getIban(String initiativeId, String userId);
  InitiativeListDTO getInitiativeList(String userId);
  void createWallet(EvaluationDTO evaluationDTO);

}
