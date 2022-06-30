package it.gov.pagopa.wallet.service;

import it.gov.pagopa.wallet.dto.EnrollmentStatusDTO;

public interface WalletService {
  void checkInitiative(String initiativeId);

  EnrollmentStatusDTO getEnrollmentStatus(String initiativeId, String userId);

  void enrollInstrument(String initiativeId, String userId, String hpan);
  void enrollIban(String initiativeId, String userId, String iban, String description);
}
