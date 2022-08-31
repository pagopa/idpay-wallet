package it.gov.pagopa.wallet.connector;

import it.gov.pagopa.wallet.dto.UnsubscribeCallDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(
    name = "${rest-client.onboarding.serviceCode}",
    url = "${onboarding.uri}")
public interface OnboardingRestClient {

  @DeleteMapping(
      value = "/idpay/onboarding/disable",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  void disableOnboarding(
      @RequestBody UnsubscribeCallDTO body);

}