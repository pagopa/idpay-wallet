package it.gov.pagopa.wallet.connector;

import it.gov.pagopa.wallet.dto.UnsubscribeCallDTO;
import it.gov.pagopa.wallet.exception.custom.OnboardingInvocationException;
import it.gov.pagopa.wallet.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.wallet.exception.custom.UserNotOnboardedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.*;
import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionMessage.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;


class OnboardingRestClientTest {

  private static final String USER_ID = "USER_ID";
  private static final String USER_ID_NOT_ONBOARDED = "USER_ID_NOT_ONBOARDED";
  private static final String USER_ID_GENERIC_ERROR = "USER_ID_GENERIC_ERROR";
  private static final String USER_ID_BAD_REQUEST = "USER_ID_BAD_REQUEST";
  private static final String INITIATIVE_ID = "INITIATIVE_ID";
  private static final String CHANNEL = "APP_IO";

  private OnboardingRestClient restClient;

  private OnboardingRestConnectorImpl restConnector;

  @BeforeEach
  void setUp() {
    restClient = Mockito.mock(OnboardingRestClient.class);
    restConnector = new OnboardingRestConnectorImpl(restClient);
  }

  @Test
  void disable_Onboarding() {
    // Given
    final UnsubscribeCallDTO unsubscribeDTO =
            new UnsubscribeCallDTO(INITIATIVE_ID, USER_ID, LocalDateTime.now().toString(), CHANNEL);

    // When
    assertDoesNotThrow(() -> restConnector.disableOnboarding(unsubscribeDTO));
  }

  @Test
  void disable_Onboarding_NOT_FOUND() {
    // Given
    final UnsubscribeCallDTO unsubscribeDTO =
            new UnsubscribeCallDTO(INITIATIVE_ID, USER_ID_NOT_ONBOARDED, LocalDateTime.now().toString(), CHANNEL);
    Mockito.doThrow(new UserNotOnboardedException("WALLET_USER_NOT_ONBOARDED","An error occurred in the microservice onboarding",null,true,new Throwable()))
            .when(restClient).disableOnboarding(any());
    // When
    UserNotOnboardedException exception = assertThrows(UserNotOnboardedException.class,
            () -> restConnector.disableOnboarding(unsubscribeDTO));

    // Then
    assertEquals(USER_NOT_ONBOARDED, exception.getCode());
  }

  @Test
  void disable_Onboarding_GENERIC_ERROR() {
    // Given
    final UnsubscribeCallDTO unsubscribeDTO =
            new UnsubscribeCallDTO(INITIATIVE_ID, USER_ID_GENERIC_ERROR, LocalDateTime.now().toString(), CHANNEL);
    Mockito.doThrow(new OnboardingInvocationException("WALLET_GENERIC_ERROR","An error occurred in the microservice onboarding",null,true,new Throwable()))
            .when(restClient).disableOnboarding(any());
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
    Mockito.doThrow(new OperationNotAllowedException("WALLET_SUSPENSION_NOT_ALLOWED_FOR_USER_STATUS","It is not possible to suspend the user on initiative [INITIATIVE_ID]",null,true,new Throwable()))
            .when(restClient).suspendOnboarding(any(),any());
    OperationNotAllowedException exception = assertThrows(OperationNotAllowedException.class,
            () -> restConnector.suspendOnboarding(INITIATIVE_ID, USER_ID_BAD_REQUEST));

    // Then
    assertEquals(SUSPENSION_NOT_ALLOWED, exception.getCode());
    assertEquals(String.format(ERROR_SUSPENSION_STATUS_MSG,INITIATIVE_ID), exception.getMessage());
  }

  @Test
  void suspend_onboarding_NOT_FOUND() {
    // When
    Mockito.doThrow(new UserNotOnboardedException("WALLET_USER_NOT_ONBOARDED","The current user is not onboarded on initiative [INITIATIVE_ID]",null,true,new Throwable()))
            .when(restClient).suspendOnboarding(any(),any());

    UserNotOnboardedException exception = assertThrows(UserNotOnboardedException.class,
            () -> restConnector.suspendOnboarding(INITIATIVE_ID, USER_ID_NOT_ONBOARDED));

    // Then
    assertEquals(USER_NOT_ONBOARDED, exception.getCode());
    assertEquals(String.format(USER_NOT_ONBOARDED_MSG,INITIATIVE_ID), exception.getMessage());
  }

  @Test
  void suspend_onboarding_GENERIC_ERROR() {
    // When
    Mockito.doThrow(new OnboardingInvocationException("WALLET_GENERIC_ERROR","An error occurred in the microservice onboarding",null,true,new Throwable()))
            .when(restClient).suspendOnboarding(any(),any());
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
    Mockito.doThrow(new OperationNotAllowedException("WALLET_READMISSION_NOT_ALLOWED_FOR_USER_STATUS","It is not possible to readmit the user on initiative [INITIATIVE_ID]",null,true,new Throwable()))
            .when(restClient).readmitOnboarding(any(),any());
    OperationNotAllowedException exception = assertThrows(OperationNotAllowedException.class,
            () -> restConnector.readmitOnboarding(INITIATIVE_ID, USER_ID_BAD_REQUEST));

    // Then
    assertEquals(READMISSION_NOT_ALLOWED, exception.getCode());
  }

  @Test
  void readmit_onboarding_NOT_FOUND() {
    // When
    Mockito.doThrow(new UserNotOnboardedException("WALLET_USER_NOT_ONBOARDED","The current user is not onboarded on initiative [INITIATIVE_ID]",null,true,new Throwable()))
            .when(restClient).readmitOnboarding(any(),any());

    UserNotOnboardedException exception = assertThrows(UserNotOnboardedException.class,
            () -> restConnector.readmitOnboarding(INITIATIVE_ID, USER_ID_NOT_ONBOARDED));

    // Then
    assertEquals(USER_NOT_ONBOARDED, exception.getCode());
    assertEquals(String.format(USER_NOT_ONBOARDED_MSG,INITIATIVE_ID), exception.getMessage());
  }

  @Test
  void readmit_onboarding_GENERIC_ERROR() {
    // When
    Mockito.doThrow(new OnboardingInvocationException("WALLET_GENERIC_ERROR","An error occurred in the microservice onboarding",null,true,new Throwable()))
            .when(restClient).readmitOnboarding(any(),any());

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
    Mockito.doThrow(new OnboardingInvocationException("WALLET_GENERIC_ERROR","An error occurred in the microservice onboarding"))
            .when(restClient).rollback(any(),any());
    OnboardingInvocationException exception = assertThrows(OnboardingInvocationException.class,
            () -> restConnector.rollback(INITIATIVE_ID, USER_ID_GENERIC_ERROR));

    // Then
    assertEquals(GENERIC_ERROR, exception.getCode());
    assertEquals(ERROR_ONBOARDING_INVOCATION_MSG, exception.getMessage());
  }


}
