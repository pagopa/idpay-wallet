package it.gov.pagopa.wallet.connector;

import it.gov.pagopa.wallet.dto.UnsubscribeCallDTO;
import org.springframework.web.bind.annotation.RequestBody;

public interface OnboardingRestConnector {


  void disableOnboarding(@RequestBody UnsubscribeCallDTO body);
}
