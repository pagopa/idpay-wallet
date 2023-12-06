package it.gov.pagopa.wallet.connector;

import feign.FeignException;
import it.gov.pagopa.wallet.dto.UnsubscribeCallDTO;
import it.gov.pagopa.wallet.exception.custom.OnboardingInvocationException;
import it.gov.pagopa.wallet.exception.custom.OperationNotAllowedException;
import it.gov.pagopa.wallet.exception.custom.UserNotOnboardedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.READMISSION_NOT_ALLOWED;
import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.SUSPENSION_NOT_ALLOWED;
import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionMessage.*;

@Service
@Slf4j
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
        log.error("[DISABLE_ONBOARDING] The user {} is not onboarded on initiative {}", body.getUserId(), body.getInitiativeId());
        throw new UserNotOnboardedException(String.format(USER_NOT_ONBOARDED_MSG, body.getInitiativeId()));
      }

      log.error("[DISABLE_ONBOARDING] An error occurred while invoking the onboarding microservice");
      throw new OnboardingInvocationException(ERROR_ONBOARDING_INVOCATION_MSG);
    }
  }

  @Override
  public void suspendOnboarding(String initiativeId, String userId) {
    try {
      onboardingRestClient.suspendOnboarding(initiativeId,userId);
    } catch (FeignException e){
      if (e.status() == 400){
        log.info("[SUSPEND_ONBOARDING] Cannot suspend user {} on initiative {} because they are not in a valid state",
                userId, initiativeId);
        throw new OperationNotAllowedException(
                SUSPENSION_NOT_ALLOWED, String.format(ERROR_SUSPENSION_STATUS_MSG, initiativeId));
      }

      if (e.status() == 404){
        log.error("[SUSPEND_ONBOARDING] The user {} is not onboarded on initiative {}", userId, initiativeId);
        throw new UserNotOnboardedException(String.format(USER_NOT_ONBOARDED_MSG, initiativeId));
      }

      log.error("[SUSPEND_ONBOARDING] An error occurred while invoking the onboarding microservice");
      throw new OnboardingInvocationException(ERROR_ONBOARDING_INVOCATION_MSG);
    }
  }
  @Override
  public void readmitOnboarding(String initiativeId, String userId) {
    try {
      onboardingRestClient.readmitOnboarding(initiativeId,userId);
    } catch (FeignException e){
      if (e.status() == 400){
        log.info("[READMIT_ONBOARDING] Cannot readmit user {} on initiative {} because they are not in a valid state",
                userId, initiativeId);
        throw new OperationNotAllowedException(
                READMISSION_NOT_ALLOWED, String.format(ERROR_READMIT_STATUS_MSG, initiativeId));
      }

      if (e.status() == 404){
        log.error("[READMIT_ONBOARDING] The user {} is not onboarded on initiative {}", userId, initiativeId);
        throw new UserNotOnboardedException(String.format(USER_NOT_ONBOARDED_MSG, initiativeId));
      }

      log.error("[READMIT_ONBOARDING] An error occurred while invoking the onboarding microservice");
      throw new OnboardingInvocationException(ERROR_ONBOARDING_INVOCATION_MSG);
    }
  }

  @Override
  public void rollback(String initiativeId, String userId) {
    try {
      onboardingRestClient.rollback(initiativeId, userId);
    } catch (FeignException e){
      log.error("[ROLLBACK] An error occurred while invoking the onboarding microservice");
      throw new OnboardingInvocationException(ERROR_ONBOARDING_INVOCATION_MSG);
    }
  }

}
