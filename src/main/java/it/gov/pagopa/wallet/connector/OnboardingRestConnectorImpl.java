package it.gov.pagopa.wallet.connector;

import feign.FeignException;
import it.gov.pagopa.wallet.dto.UnsubscribeCallDTO;
import it.gov.pagopa.wallet.exception.custom.OnboardingInvocationException;
import it.gov.pagopa.wallet.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.wallet.exception.custom.UserNotOnboardedException;
import org.springframework.stereotype.Service;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.READMISSION_NOT_ALLOWED;
import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.SUSPENSION_NOT_ALLOWED;
import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionMessage.*;

@Service
public class OnboardingRestConnectorImpl implements OnboardingRestConnector {

  private final OnboardingRestClient onboardingRestClient;

  public OnboardingRestConnectorImpl(
      OnboardingRestClient onboardingRestClient) {
    this.onboardingRestClient = onboardingRestClient;
  }


  @Override
  public void disableOnboarding(UnsubscribeCallDTO body) {
    try {
      onboardingRestClient.disableOnboarding(body);
    } catch (FeignException e){
      if (e.status() == 404){
        throw new UserNotOnboardedException(String.format(USER_NOT_ONBOARDED_MSG, body.getInitiativeId()));
      }

      throw new OnboardingInvocationException(ERROR_ONBOARDING_INVOCATION_MSG);
    }
  }

  @Override
  public void suspendOnboarding(String initiativeId, String userId) {
    try {
      onboardingRestClient.suspendOnboarding(initiativeId,userId);
    } catch (FeignException e){
      if (e.status() == 400){
        throw new OperationNotAllowedException(
                SUSPENSION_NOT_ALLOWED, String.format(ERROR_SUSPENSION_STATUS_MSG, initiativeId));
      }

      if (e.status() == 404){
        throw new UserNotOnboardedException(String.format(USER_NOT_ONBOARDED_MSG, initiativeId));
      }

      throw new OnboardingInvocationException(ERROR_ONBOARDING_INVOCATION_MSG);
    }
  }
  @Override
  public void readmitOnboarding(String initiativeId, String userId) {
    try {
      onboardingRestClient.readmitOnboarding(initiativeId,userId);
    } catch (FeignException e){
      if (e.status() == 400){
        throw new OperationNotAllowedException(
                READMISSION_NOT_ALLOWED, String.format(ERROR_READMIT_STATUS_MSG, initiativeId));
      }

      if (e.status() == 404){
        throw new UserNotOnboardedException(String.format(USER_NOT_ONBOARDED_MSG, initiativeId));
      }

      throw new OnboardingInvocationException(ERROR_ONBOARDING_INVOCATION_MSG);
    }
  }

  @Override
  public void rollback(String initiativeId, String userId) {
    try {
      onboardingRestClient.rollback(initiativeId, userId);
    } catch (FeignException e){
      throw new OnboardingInvocationException(ERROR_ONBOARDING_INVOCATION_MSG);
    }
  }

}
