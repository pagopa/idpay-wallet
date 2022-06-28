package it.gov.pagopa.wallet.service;

import it.gov.pagopa.wallet.dto.EnrollmentStatusDTO;
import it.gov.pagopa.wallet.model.Wallet;

public interface WalletService {
    void checkInitiative(String initiativeId);
    Wallet findByInitiativeIdAndUserId(String initiativeId, String userId);
    void updateEnrollmentWithNewInstrument(Wallet wallet, int nInstr);
    EnrollmentStatusDTO getEnrollmentStatus(String initiativeId, String userId);
}
