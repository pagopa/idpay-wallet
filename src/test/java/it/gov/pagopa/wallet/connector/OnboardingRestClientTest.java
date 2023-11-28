package it.gov.pagopa.wallet.connector;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import it.gov.pagopa.wallet.config.WalletConfig;
import it.gov.pagopa.wallet.dto.UnsubscribeCallDTO;
import java.time.LocalDateTime;

import it.gov.pagopa.wallet.exception.custom.OnboardingInvocationException;
import it.gov.pagopa.wallet.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.wallet.exception.custom.UserNotOnboardedException;
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
    initializers = OnboardingRestClientTest.WireMockInitializer.class,
    classes = {
      OnboardingRestConnectorImpl.class,
      WalletConfig.class,
      FeignAutoConfiguration.class,
      HttpMessageConvertersAutoConfiguration.class
    })
@TestPropertySource(
    locations = "classpath:application.yml",
    properties = {"spring.application.name=idpay-onboarding-integration-rest"})
class OnboardingRestClientTest {

  private static final String USER_ID = "USER_ID";
  private static final String USER_ID_NOT_ONBOARDED = "USER_ID_NOT_ONBOARDED";
  private static final String USER_ID_GENERIC_ERROR = "USER_ID_GENERIC_ERROR";
  private static final String USER_ID_BAD_REQUEST = "USER_ID_BAD_REQUEST";
  private static final String INITIATIVE_ID = "INITIATIVE_ID";

  @Autowired private OnboardingRestClient restClient;

  @Autowired private OnboardingRestConnector restConnector;

  @Test
  void disable_Onboarding() {
    // Given
    final UnsubscribeCallDTO unsubscribeDTO =
            new UnsubscribeCallDTO(INITIATIVE_ID, USER_ID, LocalDateTime.now().toString());

    // When
    assertDoesNotThrow(() -> restConnector.disableOnboarding(unsubscribeDTO));
  }

  @Test
  void disable_Onboarding_NOT_FOUND() {
    // Given
    final UnsubscribeCallDTO unsubscribeDTO =
            new UnsubscribeCallDTO(INITIATIVE_ID, USER_ID_NOT_ONBOARDED, LocalDateTime.now().toString());

    // When
    UserNotOnboardedException exception = assertThrows(UserNotOnboardedException.class,
            () -> restConnector.disableOnboarding(unsubscribeDTO));

    // Then
    assertEquals(USER_NOT_ONBOARDED, exception.getCode());
    assertEquals(String.format(USER_NOT_ONBOARDED_MSG, INITIATIVE_ID), exception.getMessage());
  }

  @Test
  void disable_Onboarding_GENERIC_ERROR() {
    // Given
    final UnsubscribeCallDTO unsubscribeDTO =
            new UnsubscribeCallDTO(INITIATIVE_ID, USER_ID_GENERIC_ERROR, LocalDateTime.now().toString());

    // When
    OnboardingInvocationException exception = assertThrows(OnboardingInvocationException.class,
            () -> restConnector.disableOnboarding(unsubscribeDTO));

    // Then
    assertEquals(GENERIC_ERROR, exception.getCode());
    assertEquals(ERROR_ONBOARDING_INVOCATION_MSG, exception.getMessage());
  }

  @Test
  void suspend_onboarding() {
    assertDoesNotThrow(() -> restConnector.suspendOnboarding(INITIATIVE_ID, USER_ID));
  }

  @Test
  void suspend_onboarding_BAD_REQUEST() {
    // When
    OperationNotAllowedException exception = assertThrows(OperationNotAllowedException.class,
            () -> restConnector.suspendOnboarding(INITIATIVE_ID, USER_ID_BAD_REQUEST));

    // Then
    assertEquals(SUSPENSION_NOT_ALLOWED, exception.getCode());
    assertEquals(String.format(ERROR_SUSPENSION_STATUS_MSG,INITIATIVE_ID), exception.getMessage());
  }

  @Test
  void suspend_onboarding_NOT_FOUND() {
    // When
    UserNotOnboardedException exception = assertThrows(UserNotOnboardedException.class,
            () -> restConnector.suspendOnboarding(INITIATIVE_ID, USER_ID_NOT_ONBOARDED));

    // Then
    assertEquals(USER_NOT_ONBOARDED, exception.getCode());
    assertEquals(String.format(USER_NOT_ONBOARDED_MSG,INITIATIVE_ID), exception.getMessage());
  }

  @Test
  void suspend_onboarding_GENERIC_ERROR() {
    // When
    OnboardingInvocationException exception = assertThrows(OnboardingInvocationException.class,
            () -> restConnector.suspendOnboarding(INITIATIVE_ID, USER_ID_GENERIC_ERROR));

    // Then
    assertEquals(GENERIC_ERROR, exception.getCode());
    assertEquals(ERROR_ONBOARDING_INVOCATION_MSG, exception.getMessage());
  }

  @Test
  void readmit_onboarding() {
    assertDoesNotThrow(() -> restConnector.readmitOnboarding(INITIATIVE_ID, USER_ID));
  }

  @Test
  void readmit_onboarding_BAD_REQUEST() {
    // When
    OperationNotAllowedException exception = assertThrows(OperationNotAllowedException.class,
            () -> restConnector.readmitOnboarding(INITIATIVE_ID, USER_ID_BAD_REQUEST));

    // Then
    assertEquals(READMISSION_NOT_ALLOWED, exception.getCode());
    assertEquals(String.format(ERROR_READMIT_STATUS_MSG,INITIATIVE_ID), exception.getMessage());
  }

  @Test
  void readmit_onboarding_NOT_FOUND() {
    // When
    UserNotOnboardedException exception = assertThrows(UserNotOnboardedException.class,
            () -> restConnector.readmitOnboarding(INITIATIVE_ID, USER_ID_NOT_ONBOARDED));

    // Then
    assertEquals(USER_NOT_ONBOARDED, exception.getCode());
    assertEquals(String.format(USER_NOT_ONBOARDED_MSG,INITIATIVE_ID), exception.getMessage());
  }

  @Test
  void readmit_onboarding_GENERIC_ERROR() {
    // When
    OnboardingInvocationException exception = assertThrows(OnboardingInvocationException.class,
            () -> restConnector.readmitOnboarding(INITIATIVE_ID, USER_ID_GENERIC_ERROR));

    // Then
    assertEquals(GENERIC_ERROR, exception.getCode());
    assertEquals(ERROR_ONBOARDING_INVOCATION_MSG, exception.getMessage());
  }

  @Test
  void rollback_onboarding() {
    assertDoesNotThrow(() -> restConnector.rollback(INITIATIVE_ID, USER_ID));
  }

  @Test
  void rollback_onboarding_GENERIC_ERROR() {
    // When
    OnboardingInvocationException exception = assertThrows(OnboardingInvocationException.class,
            () -> restConnector.rollback(INITIATIVE_ID, USER_ID_GENERIC_ERROR));

    // Then
    assertEquals(GENERIC_ERROR, exception.getCode());
    assertEquals(ERROR_ONBOARDING_INVOCATION_MSG, exception.getMessage());
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
