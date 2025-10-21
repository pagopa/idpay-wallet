package it.gov.pagopa.wallet.utils;

import ch.qos.logback.classic.LoggerContext;
import it.gov.pagopa.wallet.constants.WalletConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class AuditUtilitiesTest {

  private static final String MSG = "TEST_MSG";
  private static final String CHANNEL = "CHANNEL";
  private static final String USER_ID = "TEST_USER_ID";
  private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";
  private static final String IDWALLET = "IDWALLET";
  private static final String IBAN = "TEST_IBAN";

  private MemoryAppender memoryAppender;

  private final AuditUtilities auditUtilities = new AuditUtilities();

  @BeforeEach
  public void setup() {
    ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("AUDIT");
    memoryAppender = new MemoryAppender();
    memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
    logger.setLevel(ch.qos.logback.classic.Level.INFO);
    logger.addAppender(memoryAppender);
    memoryAppender.start();
  }


  @Test
  void logCreateWallet_ok(){
    auditUtilities.logCreatedWallet(USER_ID, INITIATIVE_ID);

    Assertions.assertEquals(
            ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Wallet dstip=%s msg=Wallet's citizen created." +
                    " suser=%s cs1Label=initiativeId cs1=%s")
                    .formatted(
                            AuditUtilities.SRCIP,
                            USER_ID,
                            INITIATIVE_ID
                    ),
            memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
    );
  }

  @Test
  void logCreateWallet_KoJoined(){
    auditUtilities.logCreateWalletStoppedForJoin(USER_ID, INITIATIVE_ID);

    Assertions.assertEquals(
            ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Wallet dstip=%s msg=Wallet's citizen not created due to pre-existing onboarded family member on the initative." +
                    " suser=%s cs1Label=initiativeId cs1=%s")
                    .formatted(
                            AuditUtilities.SRCIP,
                            USER_ID,
                            INITIATIVE_ID
                    ),
            memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
    );
  }


  @Test
  void logEnrollInstrument_ok(){
    auditUtilities.logEnrollmentInstrument(USER_ID, INITIATIVE_ID, IDWALLET, CHANNEL);

    Assertions.assertEquals(
            ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Wallet dstip=%s msg=Request for association of an instrument to an initiative from " + CHANNEL +
                    " suser=%s cs1Label=initiativeId cs1=%s cs3Label=idWallet cs3=%s")
                    .formatted(
                            AuditUtilities.SRCIP,
                            USER_ID,
                            INITIATIVE_ID,
                            IDWALLET
                    ),
            memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
    );
  }

  @Test
  void logEnrollInstrumentIssuer_ok(){
    auditUtilities.logEnrollmentInstrumentIssuer(USER_ID,INITIATIVE_ID,CHANNEL);

    Assertions.assertEquals(
            ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Wallet dstip=%s msg=Request for association of an instrument to an initiative from Issuer." +
                    " suser=%s cs1Label=initiativeId cs1=%s cs2Label=channel cs2=%s")
                    .formatted(
                            AuditUtilities.SRCIP,
                            USER_ID,
                            INITIATIVE_ID,
                            CHANNEL
                    ),
            memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
    );
  }
  @Test
  void logEnrollmentInstrumentKO_ok(){
    auditUtilities.logEnrollmentInstrumentKO(USER_ID,INITIATIVE_ID,IDWALLET,MSG);

    Assertions.assertEquals(
            ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Wallet dstip=%s msg=Request for association of an instrument to an initiative failed:" +
                    " %s suser=%s cs1Label=initiativeId cs1=%s cs3Label=idWallet cs3=%s")
                    .formatted(
                            AuditUtilities.SRCIP,
                            MSG,
                            USER_ID,
                            INITIATIVE_ID,
                            IDWALLET
                    ),
            memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
    );
  }
  @Test
  void logEnrollIban_ok(){
    auditUtilities.logEnrollmentIban(USER_ID,INITIATIVE_ID,CHANNEL);

    Assertions.assertEquals(
            ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Wallet dstip=%s msg=Request for association of an IBAN to an initiative." +
                    " suser=%s cs1Label=initiativeId cs1=%s cs2Label=channel cs2=%s")
                    .formatted(
                            AuditUtilities.SRCIP,
                            USER_ID,
                            INITIATIVE_ID,
                            CHANNEL
                    ),
            memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
    );
  }
  @Test
  void logEnrollIbanKO_ok(){
    auditUtilities.logEnrollmentIbanKO(MSG, USER_ID,INITIATIVE_ID,CHANNEL);

    Assertions.assertEquals(
            ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Wallet dstip=%s msg=Request for association of an IBAN to an initiative failed:" +
                    " %s suser=%s cs1Label=initiativeId cs1=%s cs2Label=channel cs2=%s")
                    .formatted(
                            AuditUtilities.SRCIP,
                            MSG,
                            USER_ID,
                            INITIATIVE_ID,
                            CHANNEL
                    ),
            memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
    );
  }
  @Test
  void logEnrollmentIbanValidationKO_ok(){
    auditUtilities.logEnrollmentIbanValidationKO(IBAN);

    Assertions.assertEquals(
            ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Wallet dstip=%s msg=The iban %s is not valid.")
                    .formatted(
                            AuditUtilities.SRCIP,
                            IBAN
                    ),
            memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
    );
  }
  @Test
  void logEnrollmentIbanComplete_ok(){
    auditUtilities.logEnrollmentIbanComplete(USER_ID,INITIATIVE_ID, IBAN);

    Assertions.assertEquals(
            ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Wallet dstip=%s msg=The iban %s was associated to initiative." +
                    " suser=%s cs1Label=initiativeId cs1=%s")
                    .formatted(
                            AuditUtilities.SRCIP,
                            IBAN,
                            USER_ID,
                            INITIATIVE_ID
                    ),
            memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
    );
  }

  @Test
  void logIbanDeleted_ok(){
    auditUtilities.logIbanDeleted(USER_ID,INITIATIVE_ID,IBAN);

    Assertions.assertEquals(
            ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Wallet dstip=%s msg=The iban %s was disassociated from initiative." +
                    " suser=%s cs1Label=initiativeId cs1=%s")
                    .formatted(
                            AuditUtilities.SRCIP,
                            IBAN,
                            USER_ID,
                            INITIATIVE_ID
                    ),
            memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
    );
  }
  @Test
  void logIbanDeletedKO_ok(){
    auditUtilities.logIbanDeletedKO(USER_ID,INITIATIVE_ID,IBAN,MSG);

    Assertions.assertEquals(
            ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Wallet dstip=%s msg=Request to delete iban %s from initiative failed:" +
                    " %s suser=%s cs1Label=initiativeId cs1=%s")
                    .formatted(
                            AuditUtilities.SRCIP,
                            IBAN,
                            MSG,
                            USER_ID,
                            INITIATIVE_ID
                    ),
            memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
    );
  }
  @Test
  void logDeleteInstrument_ok(){
    auditUtilities.logInstrumentDeleted(USER_ID,INITIATIVE_ID);

    Assertions.assertEquals(
            ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Wallet dstip=%s msg=Request to delete an instrument from an initiative." +
                    " suser=%s cs1Label=initiativeId cs1=%s")
                    .formatted(
                            AuditUtilities.SRCIP,
                            USER_ID,
                            INITIATIVE_ID
                    ),
            memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
    );  }

  @Test
  void logUnsubscribe_ok(){
    auditUtilities.logUnsubscribe(USER_ID,INITIATIVE_ID);

    Assertions.assertEquals(
            ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Wallet dstip=%s msg=Wallet unsubscribed." +
                    " suser=%s cs1Label=initiativeId cs1=%s")
                    .formatted(
                            AuditUtilities.SRCIP,
                            USER_ID,
                            INITIATIVE_ID
                    ),
            memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
    );
  }
  @Test
  void logUnsubscribeKO_ok(){
    auditUtilities.logUnsubscribeKO(USER_ID,INITIATIVE_ID,MSG);

    Assertions.assertEquals(
            ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Wallet dstip=%s msg=Request of unsubscription from initiative failed:" +
                    " %s suser=%s cs1Label=initiativeId cs1=%s")
                    .formatted(
                            AuditUtilities.SRCIP,
                            MSG,
                            USER_ID,
                            INITIATIVE_ID
                    ),
            memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
    );
  }
  @Test
  void logSuspension_ok(){
    auditUtilities.logSuspension(USER_ID,INITIATIVE_ID);

    Assertions.assertEquals(
            ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Wallet dstip=%s msg=Wallet suspended" +
                    " suser=%s cs1Label=initiativeId cs1=%s")
                    .formatted(
                            AuditUtilities.SRCIP,
                            USER_ID,
                            INITIATIVE_ID
                    ),
            memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
    );
  }

  @Test
  void logSuspensionKO_ok(){
    auditUtilities.logSuspensionKO(USER_ID,INITIATIVE_ID);

    Assertions.assertEquals(
            ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Wallet dstip=%s msg=Wallet suspension failed" +
                    " suser=%s cs1Label=initiativeId cs1=%s")
                    .formatted(
                            AuditUtilities.SRCIP,
                            USER_ID,
                            INITIATIVE_ID
                    ),
            memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
    );
  }

  @Test
  void logReadmission_ok(){
    auditUtilities.logReadmission(USER_ID,INITIATIVE_ID);

    Assertions.assertEquals(
            ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Wallet dstip=%s msg=Wallet readmitted" +
                    " suser=%s cs1Label=initiativeId cs1=%s")
                    .formatted(
                            AuditUtilities.SRCIP,
                            USER_ID,
                            INITIATIVE_ID
                    ),
            memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
    );
  }

  @Test
  void logReadmissionKO_ok(){
    auditUtilities.logReadmissionKO(USER_ID,INITIATIVE_ID);

    Assertions.assertEquals(
            ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Wallet dstip=%s msg=Wallet readmission failed" +
                    " suser=%s cs1Label=initiativeId cs1=%s")
                    .formatted(
                            AuditUtilities.SRCIP,
                            USER_ID,
                            INITIATIVE_ID
                    ),
            memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
    );
  }

  @Test
  void deleteWallets(){
    auditUtilities.logDeletedWallet(USER_ID, INITIATIVE_ID);

    Assertions.assertEquals(
            ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Wallet dstip=%s msg=Wallet deleted" +
                    " suser=%s cs1Label=initiativeId cs1=%s")
                    .formatted(
                            AuditUtilities.SRCIP,
                            USER_ID,
                            INITIATIVE_ID
                    ),
            memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
    );
  }

  @Test
  void logEnrollInstrumentCode(){
    auditUtilities.logEnrollmentInstrumentCode(USER_ID, INITIATIVE_ID, CHANNEL);

    Assertions.assertEquals(
            ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Wallet dstip=%s msg=Request for association of idpay code to an initiative." +
                    " suser=%s cs1Label=initiativeId cs1=%s cs2Label=channel cs2=%s cs3Label=instrumentType cs3=%s")
                    .formatted(
                            AuditUtilities.SRCIP,
                            USER_ID,
                            INITIATIVE_ID,
                            CHANNEL,
                            WalletConstants.INSTRUMENT_TYPE_IDPAYCODE
                    ),
            memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
    );
  }

  @Test
  void logEnrollInstrumentCodeKO(){
    auditUtilities.logEnrollmentInstrumentCodeKO(USER_ID, INITIATIVE_ID, MSG, CHANNEL);

    Assertions.assertEquals(
            ("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Wallet dstip=%s msg=Request for association of idpay code to an initiative failed: %s" +
                    " suser=%s cs1Label=initiativeId cs1=%s cs2Label=channel cs2=%s cs3Label=instrumentType cs3=%s")
                    .formatted(
                            AuditUtilities.SRCIP,
                            MSG,
                            USER_ID,
                            INITIATIVE_ID,
                            CHANNEL,
                            WalletConstants.INSTRUMENT_TYPE_IDPAYCODE
                    ),
            memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
    );
  }

}
