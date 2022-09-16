package it.gov.pagopa.wallet.connector;

import it.gov.pagopa.wallet.dto.UnsubscribeCallDTO;
import org.springframework.stereotype.Service;

@Service
public class OnboardingRestConnectorImpl implements OnboardingRestConnector {

  private final OnboardingRestClient onboardingRestClient;

  public OnboardingRestConnectorImpl(
      OnboardingRestClient onboardingRestClient) {
    this.onboardingRestClient = onboardingRestClient;
  }


  @Override
  public void disableOnboarding(UnsubscribeCallDTO body) {
    onboardingRestClient.disableOnboarding(body);
  }

  @Override
  public void rollback(String initiativeId, String userId) {
  onboardingRestClient.rollback(initiativeId, userId);
  }

}
