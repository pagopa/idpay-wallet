package it.gov.pagopa.wallet.connector;

import feign.FeignException;
import it.gov.pagopa.wallet.exception.custom.PaymentInvocationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionMessage.ERROR_PAYMENT_INVOCATION_MSG;

@Service
@Slf4j
public class RewardCalculatorRestConnectorImpl implements RewardCalculatorRestConnector {

  private final RewardCalculatorRestClient rewardCalculatorRestClient;

  public RewardCalculatorRestConnectorImpl(RewardCalculatorRestClient rewardCalculatorRestClient) {
    this.rewardCalculatorRestClient = rewardCalculatorRestClient;
  }

  @Override
  public void createOnboardingCounters(String initiativeId, String userId) {
    try {
      rewardCalculatorRestClient.createOnboardingCounters(initiativeId, userId);
    } catch (FeignException e) {
      log.error("[CREATE_ONBOARDING_COUNTERS] An error occurred while invoking reward calculator for initiative {} and user {}",
          initiativeId, userId);
      throw new PaymentInvocationException(ERROR_PAYMENT_INVOCATION_MSG, true, e);
    }
  }
}
