package it.gov.pagopa.wallet.connector;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import feign.FeignException;
import feign.Request;
import feign.Response;
import it.gov.pagopa.wallet.exception.custom.RewardCalculatorInvocationException;

class RewardCalculatorRestClientTest {

  private static final String INITIATIVE_ID = "INITIATIVE_ID";
  private static final String USER_ID = "USER_ID";

  private RewardCalculatorRestClient restClient;
  private RewardCalculatorRestConnectorImpl restConnector;

  @BeforeEach
  void setUp() {
    restClient = mock(RewardCalculatorRestClient.class);
    restConnector = new RewardCalculatorRestConnectorImpl(restClient);
  }

  @Test
  void createOnboardingCounters_success() {
    doNothing().when(restClient).createOnboardingCounters(INITIATIVE_ID, USER_ID);

    assertDoesNotThrow(() -> restConnector.createOnboardingCounters(INITIATIVE_ID, USER_ID));
    verify(restClient, times(1)).createOnboardingCounters(INITIATIVE_ID, USER_ID);
  }

  @Test
  void createOnboardingCounters_error_throwsRewardCalculatorInvocationException() {
    FeignException feign500 = FeignException.errorStatus(
        "createOnboardingCounters",
        Response.builder()
        .request(Request.create(Request.HttpMethod.PUT, "", Map.of(), null, null, null))
            .status(500)
            .reason("Internal Server Error")
            .headers(Map.of())
            .body("Error", StandardCharsets.UTF_8)
            .build()
    );

    doThrow(feign500).when(restClient).createOnboardingCounters(INITIATIVE_ID, USER_ID);

    assertThrows(RewardCalculatorInvocationException.class,
        () -> restConnector.createOnboardingCounters(INITIATIVE_ID, USER_ID));
  }
}
