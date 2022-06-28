package it.gov.pagopa.wallet.service;

import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.EnrollmentStatusDTO;
import it.gov.pagopa.wallet.exception.WalletException;
import it.gov.pagopa.wallet.model.Wallet;
import it.gov.pagopa.wallet.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class WalletServiceImpl implements WalletService {

  @Autowired WalletRepository walletRepository;

  @Override
  public void checkInitiative(String initiativeId) {
    if (initiativeId.length() < 5) {
      throw new WalletException(HttpStatus.FORBIDDEN.value(), WalletConstants.ERROR_INITIATIVE_KO);
    }
  }

  @Override
  public Wallet findByInitiativeIdAndUserId(String initiativeId, String userId) {
    return walletRepository
        .findByInitiativeIdAndUserId(initiativeId, userId)
        .orElseThrow(
            () ->
                new WalletException(
                    HttpStatus.NOT_FOUND.value(), WalletConstants.ERROR_WALLET_NOT_FOUND));
  }

  @Override
  public void updateEnrollmentWithNewInstrument(Wallet wallet, int nInstr) {
    if(nInstr == wallet.getNInstr()){
      return;
    }
    wallet.setNInstr(nInstr);
    String status = wallet.getStatus();
    switch(status){
      case WalletConstants.STATUS_NOT_REFUNDABLE:
        wallet.setStatus(WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_INSTRUMENT);
        break;
      case WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_IBAN:
        wallet.setStatus(WalletConstants.STATUS_REFUNDABLE);
        break;
      case WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_INSTRUMENT, WalletConstants.STATUS_REFUNDABLE:
        break;
      default:
        return;
    }
    walletRepository.save(wallet);
  }

  @Override
  public EnrollmentStatusDTO getEnrollmentStatus(String initiativeId, String userId) {
    Wallet wallet =
        walletRepository
            .findByInitiativeIdAndUserId(initiativeId, userId)
            .orElseThrow(
                () ->
                    new WalletException(
                        HttpStatus.NOT_FOUND.value(), WalletConstants.ERROR_WALLET_NOT_FOUND));
    return new EnrollmentStatusDTO(wallet.getStatus());
  }
}
