package it.gov.pagopa.wallet.connector;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import it.gov.pagopa.wallet.config.WalletConfig;
import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.*;

import java.time.LocalDateTime;

import it.gov.pagopa.wallet.exception.custom.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.*;
import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionMessage.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(
    initializers = PaymentInstrumentRestClientTest.WireMockInitializer.class,
    classes = {
      PaymentInstrumentRestConnectorImpl.class,
      WalletConfig.class,
      FeignAutoConfiguration.class,
      HttpMessageConvertersAutoConfiguration.class
    })
@TestPropertySource(
    locations = "classpath:application.yml",
    properties = {"spring.application.name=idpay-payment-instrument-integration-rest"})
class PaymentInstrumentRestClientTest {

  private static final String USER_ID = "USER_ID";
  private static final String USER_ID_FORBIDDEN = "USER_ID_FORBIDDEN";
  private static final String USER_ID_NOT_FOUND = "USER_ID_NOT_FOUND";
  private static final String USER_ID_GENERIC_ERROR = "USER_ID_GENERIC_ERROR";
  private static final String INITIATIVE_ID = "INITIATIVE_ID";
  private static final String ID_WALLET = "ID_WALLET";
  private static final String INSTRUMENT_ID = "INSTRUMENT_ID";
  private static final String CHANNEL = "CHANNEL";
  private static final String INSTRUMENT_TYPE = "INSTRUMENT_TYPE";

  @Autowired private PaymentInstrumentRestClient restClient;

  @Autowired private PaymentInstrumentRestConnector restConnector;

  @Test
  void enroll_instrument() {
    // Given
    final InstrumentCallBodyDTO instrumentDTO =
        new InstrumentCallBodyDTO(USER_ID, INITIATIVE_ID, ID_WALLET, CHANNEL, INSTRUMENT_TYPE);

    // When
    assertDoesNotThrow(() -> restConnector.enrollInstrument(instrumentDTO));
  }

  @Test
  void enroll_instrument_FORBIDDEN() {
    // Given
    final InstrumentCallBodyDTO instrumentDTO =
            new InstrumentCallBodyDTO(USER_ID_FORBIDDEN, INITIATIVE_ID, ID_WALLET, CHANNEL, INSTRUMENT_TYPE);

    // When
    UserNotAllowedException exception = assertThrows(UserNotAllowedException.class,
            () -> restConnector.enrollInstrument(instrumentDTO));

    // Then
    assertEquals(PAYMENT_INSTRUMENT_ALREADY_ASSOCIATED, exception.getCode());
    assertEquals(PAYMENT_INSTRUMENT_ALREADY_ASSOCIATED_MSG, exception.getMessage());
  }

  @Test
  void enroll_instrument_NOT_FOUND() {
    // Given
    final InstrumentCallBodyDTO instrumentDTO =
            new InstrumentCallBodyDTO(USER_ID_NOT_FOUND, INITIATIVE_ID, ID_WALLET, CHANNEL, INSTRUMENT_TYPE);

    // When
    PaymentInstrumentNotFoundException exception = assertThrows(PaymentInstrumentNotFoundException.class,
            () -> restConnector.enrollInstrument(instrumentDTO));

    // Then
    assertEquals(PAYMENT_INSTRUMENT_NOT_FOUND, exception.getCode());
    assertEquals(PAYMENT_INSTRUMENT_NOT_FOUND_MSG, exception.getMessage());
  }

  @Test
  void enroll_instrument_GENERIC_ERROR() {
    // Given
    final InstrumentCallBodyDTO instrumentDTO =
            new InstrumentCallBodyDTO(USER_ID_GENERIC_ERROR, INITIATIVE_ID, ID_WALLET, CHANNEL, INSTRUMENT_TYPE);

    // When
    PaymentInstrumentInvocationException exception = assertThrows(PaymentInstrumentInvocationException.class,
            () -> restConnector.enrollInstrument(instrumentDTO));

    // Then
    assertEquals(GENERIC_ERROR, exception.getCode());
    assertEquals(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG, exception.getMessage());
  }

  @Test
  void disable_all_instrument() {
    // Given
    final UnsubscribeCallDTO instrument =
            new UnsubscribeCallDTO(INITIATIVE_ID, USER_ID, LocalDateTime.now().toString(), CHANNEL);

    // When
      assertDoesNotThrow(() -> restConnector.disableAllInstrument(instrument));
  }

  @Test
  void disable_all_instrument_GENERIC_ERROR() {
    // Given
    final UnsubscribeCallDTO instrument =
            new UnsubscribeCallDTO(INITIATIVE_ID, USER_ID_GENERIC_ERROR, LocalDateTime.now().toString(), CHANNEL);

    // When
    PaymentInstrumentInvocationException exception = assertThrows(PaymentInstrumentInvocationException.class,
            () -> restConnector.disableAllInstrument(instrument));

    // Then
    assertEquals(GENERIC_ERROR, exception.getCode());
    assertEquals(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG, exception.getMessage());
  }

  @Test
  void delete_instrument() {
    // Given
    final DeactivationBodyDTO instrument =
        new DeactivationBodyDTO(USER_ID, INITIATIVE_ID, INSTRUMENT_ID, CHANNEL);

    // When
    assertDoesNotThrow(() -> restConnector.deleteInstrument(instrument));
  }

  @Test
  void delete_instrument_FORBIDDEN() {
    // Given
    final DeactivationBodyDTO instrument =
            new DeactivationBodyDTO(USER_ID_FORBIDDEN, INITIATIVE_ID, INSTRUMENT_ID, CHANNEL);

    // When
    InstrumentDeleteNotAllowedException exception = assertThrows(InstrumentDeleteNotAllowedException.class,
            () -> restConnector.deleteInstrument(instrument));

    // Then
    assertEquals(PAYMENT_INSTRUMENT_DELETE_NOT_ALLOWED, exception.getCode());
    assertEquals(PAYMENT_INSTRUMENT_DELETE_NOT_ALLOWED_MSG, exception.getMessage());
  }

  @Test
  void delete_instrument_NOT_FOUND() {
    // Given
    final DeactivationBodyDTO instrument =
            new DeactivationBodyDTO(USER_ID_NOT_FOUND, INITIATIVE_ID, INSTRUMENT_ID, CHANNEL);

    // When
    PaymentInstrumentNotFoundException exception = assertThrows(PaymentInstrumentNotFoundException.class,
            () -> restConnector.deleteInstrument(instrument));

    // Then
    assertEquals(PAYMENT_INSTRUMENT_NOT_FOUND, exception.getCode());
    assertEquals(PAYMENT_INSTRUMENT_NOT_FOUND_MSG, exception.getMessage());
  }

  @Test
  void delete_instrument_GENERIC_ERROR() {
    // Given
    final DeactivationBodyDTO instrument =
            new DeactivationBodyDTO(USER_ID_GENERIC_ERROR, INITIATIVE_ID, INSTRUMENT_ID, CHANNEL);

    // When
    PaymentInstrumentInvocationException exception = assertThrows(PaymentInstrumentInvocationException.class,
            () -> restConnector.deleteInstrument(instrument));

    // Then
    assertEquals(GENERIC_ERROR, exception.getCode());
    assertEquals(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG, exception.getMessage());
  }

  @Test
  void enroll_instrument_issuer() {
    // Given
    final InstrumentIssuerCallDTO instrument =
        new InstrumentIssuerCallDTO(INITIATIVE_ID, USER_ID,"hpan", CHANNEL, "VISA", "VISA", "***", INSTRUMENT_TYPE);

    // When
    assertDoesNotThrow(() -> restConnector.enrollInstrumentIssuer(instrument));
  }

  @Test
  void enroll_instrument_issuer_FORBIDDEN() {
    // Given
    final InstrumentIssuerCallDTO instrument =
            new InstrumentIssuerCallDTO(INITIATIVE_ID, USER_ID_FORBIDDEN,"hpan", CHANNEL, "VISA", "VISA", "***", INSTRUMENT_TYPE);

    // When
    UserNotAllowedException exception = assertThrows(UserNotAllowedException.class,
            () -> restConnector.enrollInstrumentIssuer(instrument));

    // Then
    assertEquals(PAYMENT_INSTRUMENT_ALREADY_ASSOCIATED, exception.getCode());
    assertEquals(PAYMENT_INSTRUMENT_ALREADY_ASSOCIATED_MSG, exception.getMessage());
  }

  @Test
  void enroll_instrument_issuer_GENERIC_ERROR() {
    // Given
    final InstrumentIssuerCallDTO instrument =
            new InstrumentIssuerCallDTO(INITIATIVE_ID, USER_ID_GENERIC_ERROR,"hpan", CHANNEL, "VISA", "VISA", "***", INSTRUMENT_TYPE);

    // When
    PaymentInstrumentInvocationException exception = assertThrows(PaymentInstrumentInvocationException.class,
            () -> restConnector.enrollInstrumentIssuer(instrument));

    // Then
    assertEquals(GENERIC_ERROR, exception.getCode());
    assertEquals(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG, exception.getMessage());
  }

  @Test
  void get_instrument_initiatives_detail_test() {
    // When
    InstrumentDetailDTO result = restConnector.getInstrumentInitiativesDetail(ID_WALLET, USER_ID, WalletConstants.FILTER_INSTRUMENT_STATUS_LIST);

    // Then
    assertNotNull(result);
    assertEquals(1, result.getInitiativeList().size());
  }

  @Test
  void get_instrument_initiatives_detail_NOT_FOUND() {
    // When
    PaymentInstrumentNotFoundException exception = assertThrows(PaymentInstrumentNotFoundException.class,
            () -> restConnector.getInstrumentInitiativesDetail(ID_WALLET, USER_ID_NOT_FOUND, WalletConstants.FILTER_INSTRUMENT_STATUS_LIST));

    // Then
    assertEquals(PAYMENT_INSTRUMENT_NOT_FOUND, exception.getCode());
    assertEquals(PAYMENT_INSTRUMENT_NOT_FOUND_MSG, exception.getMessage());
  }

  @Test
  void get_instrument_initiatives_detail_GENERIC_ERROR() {
    // When
    PaymentInstrumentInvocationException exception = assertThrows(PaymentInstrumentInvocationException.class,
            () -> restConnector.getInstrumentInitiativesDetail(ID_WALLET, USER_ID_GENERIC_ERROR, WalletConstants.FILTER_INSTRUMENT_STATUS_LIST));

    // Then
    assertEquals(GENERIC_ERROR, exception.getCode());
    assertEquals(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG, exception.getMessage());
  }

  @Test
  void enroll_instrument_discount() {
    // Given
    final InstrumentFromDiscountDTO instrument =
        new InstrumentFromDiscountDTO(USER_ID, INITIATIVE_ID);

    // When
    assertDoesNotThrow(() -> restConnector.enrollDiscountInitiative(instrument));
  }

  @Test
  void enroll_instrument_discount_GENERIC_ERROR() {
    // Given
    final InstrumentFromDiscountDTO instrument =
            new InstrumentFromDiscountDTO(USER_ID_GENERIC_ERROR, INITIATIVE_ID);

    // When
    PaymentInstrumentInvocationException exception = assertThrows(PaymentInstrumentInvocationException.class,
            () -> restConnector.enrollDiscountInitiative(instrument));

    // Then
    assertEquals(GENERIC_ERROR, exception.getCode());
    assertEquals(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG, exception.getMessage());
  }

  @Test
  void rollback() {
    assertDoesNotThrow(() -> restConnector.rollback(INITIATIVE_ID, USER_ID));
  }

  @Test
  void rollback_GENERIC_ERROR() {
    // When
    PaymentInstrumentInvocationException exception = assertThrows(PaymentInstrumentInvocationException.class,
            () -> restConnector.rollback(INITIATIVE_ID, USER_ID_GENERIC_ERROR));

    // Then
    assertEquals(GENERIC_ERROR, exception.getCode());
    assertEquals(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG, exception.getMessage());
  }

  @Test
  void enroll_instrument_code() {
    // Given
    final InstrumentCallBodyDTO instrument = InstrumentCallBodyDTO.builder()
            .userId(USER_ID)
            .initiativeId(INITIATIVE_ID)
            .channel(CHANNEL)
            .idWallet(ID_WALLET)
            .instrumentType(WalletConstants.INSTRUMENT_TYPE_IDPAYCODE)
            .build();

    // When
    assertDoesNotThrow(() -> restConnector.enrollInstrumentCode(instrument));
  }

  @Test
  void enroll_instrument_code_NOT_FOUND() {
    // Given
    final InstrumentCallBodyDTO instrument = InstrumentCallBodyDTO.builder()
            .userId(USER_ID_NOT_FOUND)
            .initiativeId(INITIATIVE_ID)
            .channel(CHANNEL)
            .idWallet(ID_WALLET)
            .instrumentType(WalletConstants.INSTRUMENT_TYPE_IDPAYCODE)
            .build();

    // When
    IDPayCodeNotFoundException exception = assertThrows(IDPayCodeNotFoundException.class,
            () -> restConnector.enrollInstrumentCode(instrument));

    // Then
    assertEquals(IDPAYCODE_NOT_FOUND, exception.getCode());
    assertEquals(IDPAYCODE_NOT_FOUND_MSG, exception.getMessage());
  }

  @Test
  void enroll_instrument_code_GENERIC_ERROR() {
    // Given
    final InstrumentCallBodyDTO instrument = InstrumentCallBodyDTO.builder()
            .userId(USER_ID_GENERIC_ERROR)
            .initiativeId(INITIATIVE_ID)
            .channel(CHANNEL)
            .idWallet(ID_WALLET)
            .instrumentType(WalletConstants.INSTRUMENT_TYPE_IDPAYCODE)
            .build();

    // When
    PaymentInstrumentInvocationException exception = assertThrows(PaymentInstrumentInvocationException.class,
            () -> restConnector.enrollInstrumentCode(instrument));

    // Then
    assertEquals(GENERIC_ERROR, exception.getCode());
    assertEquals(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG, exception.getMessage());
  }

  public static class WireMockInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      WireMockServer wireMockServer = new WireMockServer(new WireMockConfiguration().dynamicPort());
      wireMockServer.start();

      applicationContext.getBeanFactory().registerSingleton("wireMockServer", wireMockServer);

      applicationContext.addApplicationListener(
          applicationEvent -> {
            if (applicationEvent instanceof ContextClosedEvent) {
              wireMockServer.stop();
            }
          });

      TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
          applicationContext,
          String.format(
              "rest-client.payment.instrument.baseUrl=http://%s:%d",
              wireMockServer.getOptions().bindAddress(), wireMockServer.port()));
    }
  }
}
