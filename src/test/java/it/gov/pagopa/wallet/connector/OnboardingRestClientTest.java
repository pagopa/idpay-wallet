package it.gov.pagopa.wallet.connector;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import feign.FeignException;
import feign.Request;
import feign.Response;
import it.gov.pagopa.wallet.dto.UnsubscribeCallDTO;
import it.gov.pagopa.wallet.exception.custom.OnboardingInvocationException;
import it.gov.pagopa.wallet.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.wallet.exception.custom.UserNotOnboardedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.test.context.support.TestPropertySourceUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.USER_UNSUBSCRIBED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;

class OnboardingRestClientTest {

  private static final String USER_ID = "USER_ID";
  private static final String INITIATIVE_ID = "INITIATIVE_ID";
  private static final String CHANNEL = "APP_IO";

  private OnboardingRestClient restClient;

  private OnboardingRestConnectorImpl restConnector;


  @BeforeEach
  void setUp() {
    restClient = mock(OnboardingRestClient.class);
    restConnector = new OnboardingRestConnectorImpl(restClient);
  }

  @Test
  void disableOnboarding_success() {
    UnsubscribeCallDTO dto = new UnsubscribeCallDTO(INITIATIVE_ID, USER_ID, USER_UNSUBSCRIBED, CHANNEL);

    doNothing().when(restClient).disableOnboarding(dto);

    assertDoesNotThrow(() -> restConnector.disableOnboarding(dto));
    verify(restClient, times(1)).disableOnboarding(dto);
  }

  @Test
  void disableOnboarding_userNotOnboarded_throwsUserNotOnboardedException() {
    UnsubscribeCallDTO dto = new UnsubscribeCallDTO(INITIATIVE_ID, USER_ID, USER_UNSUBSCRIBED, CHANNEL);

    FeignException feign404 = FeignException.errorStatus(
            "disableOnboarding",
            Response.builder()
                    .request(Request.create(Request.HttpMethod.GET, "", Map.of(), null, null, null))
                    .status(404)
                    .reason("Not Found")
                    .headers(Map.of())
                    .body("Not Found", StandardCharsets.UTF_8)
                    .build()
    );

    doThrow(feign404).when(restClient).disableOnboarding(dto);

    UserNotOnboardedException ex = assertThrows(UserNotOnboardedException.class,
            () -> restConnector.disableOnboarding(dto));
    assertTrue(ex.getMessage().contains(dto.getInitiativeId()));
  }

  @Test
  void disableOnboarding_otherFeignError_throwsOnboardingInvocationException() {
    UnsubscribeCallDTO dto = new UnsubscribeCallDTO(INITIATIVE_ID, USER_ID, USER_UNSUBSCRIBED, CHANNEL);

    FeignException feign500 = FeignException.errorStatus(
            "disableOnboarding",
            Response.builder()
                    .request(Request.create(Request.HttpMethod.GET, "", Map.of(), null, null, null))
                    .status(500)
                    .reason("Internal Server Error")
                    .headers(Map.of())
                    .body("Error", StandardCharsets.UTF_8)
                    .build()
    );

    doThrow(feign500).when(restClient).disableOnboarding(dto);

    OnboardingInvocationException ex = assertThrows(OnboardingInvocationException.class,
            () -> restConnector.disableOnboarding(dto));
    assertTrue(ex.getMessage().contains("error"));
  }

  @Test
  void suspendOnboarding_success() {
    doNothing().when(restClient).suspendOnboarding("init1", "user1");
    assertDoesNotThrow(() -> restConnector.suspendOnboarding("init1", "user1"));
    verify(restClient, times(1)).suspendOnboarding("init1", "user1");
  }

  @Test
  void suspendOnboarding_badRequest_throwsOperationNotAllowedException() {
    FeignException feign400 = FeignException.errorStatus(
            "suspendOnboarding",
            Response.builder()
                    .request(Request.create(Request.HttpMethod.GET, "", Map.of(), null, null, null))
                    .status(400)
                    .reason("Bad Request")
                    .headers(Map.of())
                    .body("Bad Request", StandardCharsets.UTF_8)
                    .build()
    );

    doThrow(feign400).when(restClient).suspendOnboarding("init1", "user1");

    OperationNotAllowedException ex = assertThrows(OperationNotAllowedException.class,
            () -> restConnector.suspendOnboarding("init1", "user1"));
    assertEquals("WALLET_SUSPENSION_NOT_ALLOWED_FOR_USER_STATUS", ex.getCode());
  }

  @Test
  void suspendOnboarding_404_throwsUserNotOnboardedException() {
    FeignException feign404 = FeignException.errorStatus(
            "suspendOnboarding",
            Response.builder()
                    .request(Request.create(Request.HttpMethod.GET, "", Map.of(), null, null, null))
                    .status(404)
                    .reason("Not Found")
                    .headers(Map.of())
                    .body("Not Found", StandardCharsets.UTF_8)
                    .build()
    );

    doThrow(feign404).when(restClient).suspendOnboarding("init1", "user1");

    assertThrows(UserNotOnboardedException.class,
            () -> restConnector.suspendOnboarding("init1", "user1"));
  }

  @Test
  void suspendOnboarding_500_throwsUserNotOnboardedException() {
    FeignException feign500 = FeignException.errorStatus(
            "suspendOnboarding",
            Response.builder()
                    .request(Request.create(Request.HttpMethod.GET, "", Map.of(), null, null, null))
                    .status(500)
                    .reason("Internal Server Error")
                    .headers(Map.of())
                    .body("Internal Server Error", StandardCharsets.UTF_8)
                    .build()
    );

    doThrow(feign500).when(restClient).suspendOnboarding("init1", "user1");

    assertThrows(OnboardingInvocationException.class,
            () -> restConnector.suspendOnboarding("init1", "user1"));
  }

  @Test
  void readmitOnboarding_success() {
    doNothing().when(restClient).readmitOnboarding("init1", "user1");
    assertDoesNotThrow(() -> restConnector.readmitOnboarding("init1", "user1"));
  }

  @Test
  void readmitOnboarding_badRequest_throwsOperationNotAllowedException() {
    FeignException feign400 = FeignException.errorStatus(
            "readmitOnboarding",
            Response.builder()
                    .request(Request.create(Request.HttpMethod.GET, "", Map.of(), null, null, null))
                    .status(400)
                    .reason("Bad Request")
                    .headers(Map.of())
                    .body("Bad Request", StandardCharsets.UTF_8)
                    .build()
    );

    doThrow(feign400).when(restClient).readmitOnboarding("init1", "user1");

    OperationNotAllowedException ex = assertThrows(OperationNotAllowedException.class,
            () -> restConnector.readmitOnboarding("init1", "user1"));
    assertEquals("WALLET_READMISSION_NOT_ALLOWED_FOR_USER_STATUS", ex.getCode());
  }

  @Test
  void readmitOnboarding_404_throwsUserNotOnboardedException() {
    FeignException feign404 = FeignException.errorStatus(
            "readmitOnboarding",
            Response.builder()
                    .request(Request.create(Request.HttpMethod.GET, "", Map.of(), null, null, null))
                    .status(404)
                    .reason("Not Found")
                    .headers(Map.of())
                    .body("Not Found", StandardCharsets.UTF_8)
                    .build()
    );

    doThrow(feign404).when(restClient).readmitOnboarding("init1", "user1");

    assertThrows(UserNotOnboardedException.class,
            () -> restConnector.readmitOnboarding("init1", "user1"));
  }

  @Test
  void readmitOnboarding_500_throwsUserNotOnboardedException() {
    FeignException feign500 = FeignException.errorStatus(
            "readmitOnboarding",
            Response.builder()
                    .request(Request.create(Request.HttpMethod.GET, "", Map.of(), null, null, null))
                    .status(500)
                    .reason("Internal Server Error")
                    .headers(Map.of())
                    .body("Internal Server Error", StandardCharsets.UTF_8)
                    .build()
    );

    doThrow(feign500).when(restClient).readmitOnboarding("init1", "user1");

    assertThrows(OnboardingInvocationException.class,
            () -> restConnector.readmitOnboarding("init1", "user1"));
  }

  @Test
  void rollback_success() {
    doNothing().when(restClient).rollback("init1", "user1");
    assertDoesNotThrow(() -> restConnector.rollback("init1", "user1"));
  }

  @Test
  void rollback_anyError_throwsOnboardingInvocationException() {
    FeignException feign500 = FeignException.errorStatus(
            "rollback",
            Response.builder()
                    .request(Request.create(Request.HttpMethod.GET, "", Map.of(), null, null, null))
                    .status(500)
                    .reason("Internal Server Error")
                    .headers(Map.of())
                    .body("Error", StandardCharsets.UTF_8)
                    .build()
    );

    doThrow(feign500).when(restClient).rollback("init1", "user1");

    assertThrows(OnboardingInvocationException.class,
            () -> restConnector.rollback("init1", "user1"));
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
              "rest-client.onboarding.baseUrl=http://%s:%d",
              wireMockServer.getOptions().bindAddress(), wireMockServer.port()));
    }
  }
}
