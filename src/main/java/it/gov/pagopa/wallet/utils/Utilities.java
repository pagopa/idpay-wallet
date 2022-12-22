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

  private static final String CEF = String.format("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2|vs=172.16.151.21:80 event=Onboarding srcip=%s srcport=17548 dstip=172.16.128.37 dstport=82",
      SRCIP);
  private static final String MSG = " msg=";
  private static final String USER = "suser=";
  private static final String CS1 = "cs1Label=iniziativeId cs1=";
  private static final String CS2 = "cs2Label=channel cs2=";
  final Logger logger = Logger.getLogger("AUDIT");


  private String buildLogWithChannel(String eventLog, String userId, String initiativeId, String channel) {
    return CEF + MSG + eventLog + " " + USER + userId + " " + CS1 + initiativeId + " " + CS2 + channel;
  }

  private String buildLog(String eventLog, String userId, String initiativeId) {
    return CEF + MSG + eventLog + " " + USER + userId + " " + CS1 + initiativeId;
  }

  public void logCreatedWallet(String userId, String initiativeId) {
    String testLog = this.buildLog("Wallet's citizen created ", userId,
        initiativeId);
    logger.info(testLog);
  }

  public void logInstrumentAdded(String userId, String initiativeId, String channel) {
    String testLog = this.buildLogWithChannel("Instrument added to Wallet", userId, initiativeId, channel);
    logger.info(testLog);
  }

  public void logUnsubscribe(String userId, String initiativeId) {
    String testLog = this.buildLog("Request of unsubscription from initiative", userId, initiativeId);
    logger.info(testLog);
  }
}