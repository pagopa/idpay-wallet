package it.gov.pagopa.wallet.utils;

import it.gov.pagopa.wallet.exception.WalletException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class Utilities {
  private static final String SRCIP;

  static {
    try {
      SRCIP = InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      throw new WalletException(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }
  }

  private static final String CEF = String.format("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Wallet dstip=%s", SRCIP);
  private static final String MSG = " msg=";
  private static final String USER = "suser=";
  private static final String INITIATIVE_ID = "initiativeId";
  private static final String CHANNEL = "channel";
  private static final String ID_WALLET = "idWallet";

  final Logger logger = Logger.getLogger("AUDIT");


  private String buildLogWithChannel(String eventLog, String userId, String initiativeId, String channel) {
    return CEF + MSG + eventLog + " " + USER + userId + " " + INITIATIVE_ID + initiativeId + " " + CHANNEL + channel;
  }

  private String buildLogWithIdWallet(String eventLog, String userId, String initiativeId, String idWallet) {
    return CEF + MSG + eventLog + " " + USER + userId + " " + INITIATIVE_ID + initiativeId + " " + ID_WALLET + idWallet;
  }

  private String buildLog(String eventLog, String userId, String initiativeId) {
    return CEF + MSG + eventLog + " " + USER + userId + " " + INITIATIVE_ID + initiativeId;
  }

  public void logCreatedWallet(String userId, String initiativeId) {
    String testLog = this.buildLog("Wallet's citizen created ", userId, initiativeId);
    logger.info(testLog);
  }

  public void logEnrollmentInstrument(String userId, String initiativeId, String idWallet) {
    String testLog = this.buildLogWithIdWallet("Request for association of an instrument to an initiative from APP IO ", userId, initiativeId, idWallet);
    logger.info(testLog);
  }

  public void logEnrollmentInstrumentIssuer(String userId, String initiativeId, String channel) {
    String testLog = this.buildLogWithChannel("Request for association of an instrument to an initiative from ISSUER ", userId, initiativeId, channel);
    logger.info(testLog);
  }
  public void logEnrollmentInstrumentKO(String userId, String initiativeId, String idWallet, String msg) {
    String testLog = this.buildLogWithIdWallet("Request for association of an instrument to an initiative failed: " + msg, userId, initiativeId, idWallet);
    logger.info(testLog);
  }

  public void logEnrollmentIban(String userId, String initiativeId, String channel) {
    String testLog = this.buildLogWithChannel("Request for association of an IBAN to an initiative ", userId, initiativeId, channel);
    logger.info(testLog);
  }
  public void logEnrollmentIbanKO(String msg, String userId, String initiativeId, String channel) {
    String testLog = this.buildLogWithChannel("Request for association of an IBAN to an initiative failed: " + msg, userId, initiativeId, channel);
    logger.info(testLog);
  }
  public void logEnrollmentIbanValidationKO(String iban) {
    String testLog = CEF + MSG + String.format("The iban %s is not valid", iban);
    logger.info(testLog);
  }
  public void logEnrollmentIbanComplete(String userId, String initiativeId, String iban) {
    String testLog = this.buildLog(String.format("The iban %s was associated to initiative ", iban), userId, initiativeId);
    logger.info(testLog);
  }
  public void logIbanDeleted(String userId, String initiativeId, String iban) {
    String testLog = this.buildLog(String.format("The iban %s was disassociated from initiative ", iban), userId, initiativeId);
    logger.info(testLog);
  }
  public void logIbanDeletedKO(String userId, String initiativeId, String iban, String msg) {
    String testLog = this.buildLog(String.format("Request to delete iban %s from initiative failed:", iban) + msg, userId, initiativeId);
    logger.info(testLog);
  }

  public void logInstrumentDeleted(String userId, String initiativeId) {
    String testLog = this.buildLog("Request to delete an instrument from an initiative ", userId, initiativeId);
    logger.info(testLog);
  }

  public void logUnsubscribe(String userId, String initiativeId) {
    String testLog = this.buildLog("Request of unsubscription from initiative ", userId, initiativeId);
    logger.info(testLog);
  }
  public void logUnsubscribeKO(String userId, String initiativeId, String msg) {
    String testLog = this.buildLog("Request of unsubscription from initiative failed " + msg, userId, initiativeId);
    logger.info(testLog);
  }
}