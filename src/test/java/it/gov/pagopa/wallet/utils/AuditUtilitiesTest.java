package it.gov.pagopa.wallet.utils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import it.gov.pagopa.wallet.exception.WalletException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = {AuditUtilities.class,InetAddress.class})
class AuditUtilitiesTest {
  private static final String SRCIP;

  static {
    try {
      SRCIP = InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      throw new WalletException(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }
  }

  private static final String MSG = " TEST_MSG";
  private static final String CHANNEL = "CHANNEL";
  private static final String USER_ID = "TEST_USER_ID";
  private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";

  private static final String IDWALLET = "IDWALLET";


  @MockBean
  Logger logger;
  @Autowired
  AuditUtilities auditUtilities;
  @MockBean
  InetAddress inetAddress;
  MemoryAppender memoryAppender;

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
    auditUtilities.logCreatedWallet(USER_ID,INITIATIVE_ID);
    assertThat(memoryAppender.contains(ch.qos.logback.classic.Level.DEBUG,MSG)).isFalse();
  }

  @Test
  void logUnsubscribe_ok(){
    auditUtilities.logUnsubscribe(USER_ID,INITIATIVE_ID);
    assertThat(memoryAppender.contains(ch.qos.logback.classic.Level.DEBUG,MSG)).isFalse();
  }
  @Test
  void logUnsubscribeKO_ok(){
    auditUtilities.logUnsubscribeKO(USER_ID,INITIATIVE_ID,MSG);
    assertThat(memoryAppender.contains(ch.qos.logback.classic.Level.DEBUG,MSG)).isFalse();
  }
  @Test
  void logEnrollInstrument_ok(){
    auditUtilities.logEnrollmentInstrument(USER_ID,INITIATIVE_ID,IDWALLET);
    assertThat(memoryAppender.contains(ch.qos.logback.classic.Level.DEBUG,MSG)).isFalse();
  }

  @Test
  void logEnrollInstrumentIssuer_ok(){
    auditUtilities.logEnrollmentInstrumentIssuer(USER_ID,INITIATIVE_ID,CHANNEL);
    assertThat(memoryAppender.contains(ch.qos.logback.classic.Level.DEBUG,MSG)).isFalse();
  }
  @Test
  void logEnrollmentInstrumentKO_ok(){
    auditUtilities.logEnrollmentInstrumentKO(USER_ID,INITIATIVE_ID,CHANNEL,MSG);
    assertThat(memoryAppender.contains(ch.qos.logback.classic.Level.DEBUG,MSG)).isFalse();
  }
  @Test
  void logEnrollIban_ok(){
    auditUtilities.logEnrollmentIban(USER_ID,INITIATIVE_ID,CHANNEL);
    assertThat(memoryAppender.contains(ch.qos.logback.classic.Level.DEBUG,MSG)).isFalse();
  }
  @Test
  void logEnrollIbanKO_ok(){
    auditUtilities.logEnrollmentIbanKO(MSG, USER_ID,INITIATIVE_ID,CHANNEL);
    assertThat(memoryAppender.contains(ch.qos.logback.classic.Level.DEBUG,MSG)).isFalse();
  }
  @Test
  void logEnrollmentIbanValidationKOO_ok(){
    auditUtilities.logEnrollmentIbanValidationKO(MSG);
    assertThat(memoryAppender.contains(ch.qos.logback.classic.Level.DEBUG,MSG)).isFalse();
  }
  @Test
  void logEnrollmentIbanComplete_ok(){
    auditUtilities.logEnrollmentIbanComplete(USER_ID,INITIATIVE_ID, MSG);
    assertThat(memoryAppender.contains(ch.qos.logback.classic.Level.DEBUG,MSG)).isFalse();
  }

  @Test
  void logIbanDeleted_ok(){
    auditUtilities.logIbanDeleted(USER_ID,INITIATIVE_ID,MSG);
    assertThat(memoryAppender.contains(ch.qos.logback.classic.Level.DEBUG,MSG)).isFalse();
  }
  @Test
  void logIbanDeletedKO_ok(){
    auditUtilities.logIbanDeletedKO(USER_ID,INITIATIVE_ID,MSG,MSG);
    assertThat(memoryAppender.contains(ch.qos.logback.classic.Level.DEBUG,MSG)).isFalse();
  }
  @Test
  void logDeleteInstrument_ok(){
    auditUtilities.logInstrumentDeleted(USER_ID,INITIATIVE_ID);
    assertThat(memoryAppender.contains(ch.qos.logback.classic.Level.DEBUG,MSG)).isFalse();
  }

  public static class MemoryAppender extends ListAppender<ILoggingEvent> {
    public void reset() {
      this.list.clear();
    }

    public boolean contains(ch.qos.logback.classic.Level level, String string) {
      return this.list.stream()
          .anyMatch(event -> event.toString().contains(string)
              && event.getLevel().equals(level));
    }

    public int countEventsForLogger(String loggerName) {
      return (int) this.list.stream()
          .filter(event -> event.getLoggerName().contains(loggerName))
          .count();
    }

    public List<ILoggingEvent> search() {
      return this.list.stream()
          .filter(event -> event.toString().contains(MSG))
          .collect(Collectors.toList());
    }

    public List<ILoggingEvent> search(Level level) {
      return this.list.stream()
          .filter(event -> event.toString().contains(MSG)
              && event.getLevel().equals(level))
          .collect(Collectors.toList());
    }

    public int getSize() {
      return this.list.size();
    }

    public List<ILoggingEvent> getLoggedEvents() {
      return Collections.unmodifiableList(this.list);
    }
  }

}
