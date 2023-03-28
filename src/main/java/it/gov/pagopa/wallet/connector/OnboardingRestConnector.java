package it.gov.pagopa.wallet.connector;

import it.gov.pagopa.wallet.dto.UnsubscribeCallDTO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface OnboardingRestConnector {


  void disableOnboarding(@RequestBody UnsubscribeCallDTO body);

  void suspendOnboarding(@PathVariable String initiativeId,@PathVariable String userId);

  void rollback(@PathVariable String initiativeId,@PathVariable String userId);
}
