package it.gov.pagopa.wallet.utils;

import it.gov.pagopa.wallet.constants.WalletConstants;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
@AllArgsConstructor
@Slf4j(topic = "AUDIT")
public class AuditUtilities {
  public static final String SRCIP;

  static {
    String srcIp;
    try {
      srcIp = InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      log.error("Cannot determine the ip of the current host", e);
      srcIp="UNKNOWN";
    }

    SRCIP = srcIp;
  }

  private static final String CEF = String.format("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Wallet dstip=%s", SRCIP);
  private static final String CEF_BASE_PATTERN = CEF + " msg={}";
  private static final String CEF_PATTERN = CEF_BASE_PATTERN + " suser={} cs1Label=initiativeId cs1={}";
  private static final String CEF_PATTERN_CHANNEL = CEF_PATTERN + " cs2Label=channel cs2={}";
  private static final String CEF_PATTERN_ID_WALLET = CEF_PATTERN + " cs3Label=idWallet cs3={}";
  private static final String CEF_PATTERN_INSTRUMENT_TYPE = CEF_PATTERN_CHANNEL + " cs3Label=instrumentType cs3={}";

  private void logAuditString(String pattern, String... parameters) {
    log.info(pattern, (Object[]) parameters);
  }

  public void logCreatedWallet(String userId, String initiativeId) {
    logAuditString(
            CEF_PATTERN,
              "Wallet's citizen created.", userId, initiativeId
    );
  }

  public void logEnrollmentInstrument(String userId, String initiativeId, String idWallet) {
    logAuditString(
            CEF_PATTERN_ID_WALLET,
            "Request for association of an instrument to an initiative from APP IO.", userId, initiativeId, idWallet
    );
  }

  public void logEnrollmentInstrumentIssuer(String userId, String initiativeId, String channel) {
    logAuditString(
            CEF_PATTERN_CHANNEL,
            "Request for association of an instrument to an initiative from Issuer.", userId, initiativeId, channel
    );
  }
  public void logEnrollmentInstrumentKO(String userId, String initiativeId, String idWallet, String msg) {
    logAuditString(
            CEF_PATTERN_ID_WALLET,
            "Request for association of an instrument to an initiative failed: " + msg, userId, initiativeId, idWallet
    );
  }

  public void logEnrollmentIban(String userId, String initiativeId, String channel) {
    logAuditString(
            CEF_PATTERN_CHANNEL,
            "Request for association of an IBAN to an initiative.", userId, initiativeId, channel
    );
  }
  public void logEnrollmentIbanKO(String msg, String userId, String initiativeId, String channel) {
    logAuditString(
            CEF_PATTERN_CHANNEL,
            "Request for association of an IBAN to an initiative failed: " + msg, userId, initiativeId, channel
    );
  }
  public void logEnrollmentIbanValidationKO(String iban) {
    logAuditString(
            CEF_BASE_PATTERN,
            String.format("The iban %s is not valid.", iban)
    );
  }
  public void logEnrollmentIbanComplete(String userId, String initiativeId, String iban) {
    logAuditString(
            CEF_PATTERN,
            String.format("The iban %s was associated to initiative.", iban), userId, initiativeId
    );
  }
  public void logIbanDeleted(String userId, String initiativeId, String iban) {
    logAuditString(
            CEF_PATTERN,
            String.format("The iban %s was disassociated from initiative.", iban), userId, initiativeId
    );
  }
  public void logIbanDeletedKO(String userId, String initiativeId, String iban, String msg) {
    logAuditString(
            CEF_PATTERN,
            String.format("Request to delete iban %s from initiative failed: ", iban) + msg, userId, initiativeId
    );
  }

  public void logInstrumentDeleted(String userId, String initiativeId) {
    logAuditString(
            CEF_PATTERN,
            "Request to delete an instrument from an initiative.", userId, initiativeId
    );
  }

  public void logUnsubscribe(String userId, String initiativeId) {
    logAuditString(
            CEF_PATTERN,
            "Wallet unsubscribed.", userId, initiativeId
    );
  }
  public void logUnsubscribeKO(String userId, String initiativeId, String msg) {
    logAuditString(
            CEF_PATTERN,
            "Request of unsubscription from initiative failed: " + msg, userId, initiativeId
    );
  }

  public void logSuspension(String userId, String initiativeId) {
    logAuditString(
            CEF_PATTERN,
            "Wallet suspended", userId, initiativeId
    );
  }
  public void logSuspensionKO(String userId, String initiativeId) {
    logAuditString(
            CEF_PATTERN,
            "Wallet suspension failed", userId, initiativeId
    );
  }

  public void logReadmission(String userId, String initiativeId) {
    logAuditString(
            CEF_PATTERN,
            "Wallet readmitted", userId, initiativeId
    );
  }
  public void logReadmissionKO(String userId, String initiativeId) {
    logAuditString(
            CEF_PATTERN,
            "Wallet readmission failed", userId, initiativeId
    );
  }

  public void logDeletedWallet(String userId, String initiativeId){
    logAuditString(
            CEF_PATTERN,
            "Wallet deleted", userId,initiativeId
    );
  }

  public void logEnrollmentInstrumentCode(String userId, String initiativeId) {
    logAuditString(
            CEF_PATTERN_INSTRUMENT_TYPE,
            "Request for association of an instrument to an initiative from APP IO.", userId, initiativeId, WalletConstants.CHANNEL_APP_IO, WalletConstants.INSTRUMENT_TYPE_IDPAYCODE
    );
  }

  public void logEnrollmentInstrumentCodeKO(String userId, String initiativeId, String msg) {
    logAuditString(
            CEF_PATTERN_INSTRUMENT_TYPE,
            "Request for association of an instrument to an initiative failed: " + msg, userId, initiativeId, WalletConstants.CHANNEL_APP_IO, WalletConstants.INSTRUMENT_TYPE_IDPAYCODE
    );
  }

}