package it.gov.pagopa.wallet.connector;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import feign.FeignException;
import feign.Request;
import feign.Response;
import it.gov.pagopa.wallet.dto.UnsubscribeCallDTO;
import it.gov.pagopa.wallet.dto.DeactivationBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentCallBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentIssuerCallDTO;
import it.gov.pagopa.wallet.dto.InstrumentFromDiscountDTO;
import it.gov.pagopa.wallet.dto.InstrumentDetailDTO;
import it.gov.pagopa.wallet.exception.custom.PaymentInstrumentInvocationException;
import it.gov.pagopa.wallet.exception.custom.UserNotAllowedException;
import it.gov.pagopa.wallet.exception.custom.PaymentInstrumentNotFoundException;
import it.gov.pagopa.wallet.exception.custom.InstrumentDeleteNotAllowedException;
import it.gov.pagopa.wallet.exception.custom.IDPayCodeNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.test.context.support.TestPropertySourceUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionMessage.PAYMENT_INSTRUMENT_ALREADY_ASSOCIATED_MSG;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;


class PaymentInstrumentRestClientTest {

  private static final String USER_ID = "USER_ID";
  private static final String INITIATIVE_ID = "INITIATIVE_ID";
  private static final String ID_WALLET = "ID_WALLET";
  private static final String INSTRUMENT_ID = "INSTRUMENT_ID";
  private static final String CHANNEL = "CHANNEL";

  private PaymentInstrumentRestClient restClient;

  private PaymentInstrumentRestConnectorImpl restConnector;


  @BeforeEach
  void setUp() {
    restClient = Mockito.mock(PaymentInstrumentRestClient.class);
    restConnector = new PaymentInstrumentRestConnectorImpl(restClient);
  }

  @Test
  void enrollDiscountInitiative_ok() {
    InstrumentFromDiscountDTO body = new InstrumentFromDiscountDTO();
    assertDoesNotThrow(() -> restConnector.enrollDiscountInitiative(body));
    verify(restClient).enrollDiscountInitiative(body);
  }

  @Test
  void enrollDiscountInitiative_403() {
    InstrumentFromDiscountDTO body = new InstrumentFromDiscountDTO();

    doThrow(feignException(403))
            .when(restClient).enrollDiscountInitiative(any());

    PaymentInstrumentInvocationException ex = assertThrows(
            PaymentInstrumentInvocationException.class,
            () -> restConnector.enrollDiscountInitiative(body)
    );

    assertEquals("An error occurred in the microservice payment instrument", ex.getMessage());
  }


  @Test
  void rollback_ok() {
    assertDoesNotThrow(() -> restConnector.rollback(USER_ID,INITIATIVE_ID));
    verify(restClient).rollback(USER_ID,INITIATIVE_ID);
  }

  @Test
  void rollback_403() {

    doThrow(feignException(403))
            .when(restClient).rollback(any(),any());

    PaymentInstrumentInvocationException ex = assertThrows(
            PaymentInstrumentInvocationException.class,
            () -> restConnector.rollback(USER_ID,INITIATIVE_ID)
    );

    assertEquals("An error occurred in the microservice payment instrument", ex.getMessage());
  }

  @Test
  void enrollInstrumentIssuer_ok() {
    InstrumentIssuerCallDTO body = new InstrumentIssuerCallDTO("","","","","","","","");
    assertDoesNotThrow(() -> restConnector.enrollInstrumentIssuer(body));
    verify(restClient).enrollInstrumentIssuer(body);
  }

  @Test
  void enrollInstrumentIssuer_403() {
    InstrumentIssuerCallDTO body = new InstrumentIssuerCallDTO("","","","","","","","");

    doThrow(feignException(403))
            .when(restClient).enrollInstrumentIssuer(any());

    UserNotAllowedException ex = assertThrows(
            UserNotAllowedException.class,
            () -> restConnector.enrollInstrumentIssuer(body)
    );

    assertEquals("Payment Instrument is already associated to another user", ex.getMessage());
  }

  @Test
  void enrollInstrumentIssuer_404() {
    InstrumentIssuerCallDTO body = new InstrumentIssuerCallDTO("","","","","","","","");

    doThrow(feignException(404))
            .when(restClient).enrollInstrumentIssuer(any());

    PaymentInstrumentInvocationException ex = assertThrows(
            PaymentInstrumentInvocationException.class,
            () -> restConnector.enrollInstrumentIssuer(body)
    );

    assertEquals("An error occurred in the microservice payment instrument", ex.getMessage());
  }

  @Test
  void disableAllInstrument_ok() {
    UnsubscribeCallDTO body = new UnsubscribeCallDTO(INITIATIVE_ID, INSTRUMENT_ID,"",CHANNEL );
    assertDoesNotThrow(() -> restConnector.disableAllInstrument(body));
    verify(restClient).disableAllInstrument(body);
  }

  @Test
  void disableAllInstrument_403() {
    UnsubscribeCallDTO body = new UnsubscribeCallDTO(INITIATIVE_ID, INSTRUMENT_ID,"",CHANNEL );

    doThrow(feignException(403))
            .when(restClient).disableAllInstrument(any());

    PaymentInstrumentInvocationException ex = assertThrows(
            PaymentInstrumentInvocationException.class,
            () -> restConnector.disableAllInstrument(body)
    );

    assertEquals("An error occurred in the microservice payment instrument", ex.getMessage());
  }


  @Test
  void enrollInstrument_ok() {
    InstrumentCallBodyDTO body = new InstrumentCallBodyDTO();

    assertDoesNotThrow(() -> restConnector.enrollInstrument(body));
    verify(restClient).enrollInstrument(body);
  }

  @Test
  void enrollInstrument_403() {
    InstrumentCallBodyDTO body = new InstrumentCallBodyDTO();

    doThrow(feignException(403))
            .when(restClient).enrollInstrument(any());

    UserNotAllowedException ex = assertThrows(
            UserNotAllowedException.class,
            () -> restConnector.enrollInstrument(body)
    );

    assertEquals(PAYMENT_INSTRUMENT_ALREADY_ASSOCIATED_MSG, ex.getMessage());
  }

  @Test
  void enrollInstrument_404() {
    InstrumentCallBodyDTO body = new InstrumentCallBodyDTO();

    doThrow(feignException(404))
            .when(restClient).enrollInstrument(any());

    assertThrows(
            PaymentInstrumentNotFoundException.class,
            () -> restConnector.enrollInstrument(body)
    );
  }

  @Test
  void enrollInstrument_genericError() {
    doThrow(feignException(500))
            .when(restClient).enrollInstrument(any());

    assertThrows(
            PaymentInstrumentInvocationException.class,
            () -> restConnector.enrollInstrument(new InstrumentCallBodyDTO())
    );
  }

  @Test
  void deleteInstrument_403() {
    DeactivationBodyDTO body = new DeactivationBodyDTO(USER_ID, INITIATIVE_ID, INSTRUMENT_ID, CHANNEL);

    doThrow(feignException(403))
            .when(restClient).deleteInstrument(any());

    assertThrows(
            InstrumentDeleteNotAllowedException.class,
            () -> restConnector.deleteInstrument(body)
    );
  }

  @Test
  void deleteInstrument_404() {
    doThrow(feignException(404))
            .when(restClient).deleteInstrument(any());
    DeactivationBodyDTO body = new DeactivationBodyDTO(USER_ID, INITIATIVE_ID, INSTRUMENT_ID, CHANNEL);

    assertThrows(
            PaymentInstrumentNotFoundException.class,
            () -> restConnector.deleteInstrument(body)
    );
  }

  @Test
  void deleteInstrument_500() {
    doThrow(feignException(500))
            .when(restClient).deleteInstrument(any());
    DeactivationBodyDTO body = new DeactivationBodyDTO(USER_ID, INITIATIVE_ID, INSTRUMENT_ID, CHANNEL);

    assertThrows(
            PaymentInstrumentInvocationException.class,
            () -> restConnector.deleteInstrument(body)
    );
  }



  @Test
  void getInstrumentInitiativesDetail_ok() {
    InstrumentDetailDTO response = new InstrumentDetailDTO();
    when(restClient.getInstrumentInitiativesDetail(any(), any(), any()))
            .thenReturn(response);

    InstrumentDetailDTO result = restConnector.getInstrumentInitiativesDetail(
            ID_WALLET, USER_ID, List.of("ACTIVE")
    );

    assertNotNull(result);
  }

  @Test
  void getInstrumentInitiativesDetail_404() {
    doThrow(feignException(404))
            .when(restClient).getInstrumentInitiativesDetail(any(), any(), any());

    assertThrows(
            PaymentInstrumentNotFoundException.class,
            () -> restConnector.getInstrumentInitiativesDetail(
                    ID_WALLET, USER_ID, Collections.emptyList())
    );
  }

  @Test
  void getInstrumentInitiativesDetail_500() {
    doThrow(feignException(500))
            .when(restClient).getInstrumentInitiativesDetail(any(), any(), any());

    assertThrows(
            PaymentInstrumentInvocationException.class,
            () -> restConnector.getInstrumentInitiativesDetail(
                    ID_WALLET, USER_ID, Collections.emptyList())
    );
  }

  @Test
  void enrollInstrumentCode_404() {
    doThrow(feignException(404))
            .when(restClient).enrollInstrumentCode(any());

    assertThrows(
            IDPayCodeNotFoundException.class,
            () -> restConnector.enrollInstrumentCode(new InstrumentCallBodyDTO())
    );
  }

  @Test
  void enrollInstrumentCode_500() {
    doThrow(feignException(500))
            .when(restClient).enrollInstrumentCode(any());

    assertThrows(
            PaymentInstrumentInvocationException.class,
            () -> restConnector.enrollInstrumentCode(new InstrumentCallBodyDTO())
    );
  }

  private FeignException feignException(int status) {
    return FeignException.errorStatus(
            "method",
            Response.builder()
                    .status(status)
                    .request(
                            Request.create(
                                    Request.HttpMethod.POST,
                                    "/test",
                                    Collections.emptyMap(),
                                    new byte[0],
                                    StandardCharsets.UTF_8
                            )
                    )
                    .build()
    );
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
