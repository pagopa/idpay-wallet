package it.gov.pagopa.wallet.connector;

import it.gov.pagopa.wallet.dto.UnsubscribeCallDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(
    name = "${rest-client.onboarding.serviceCode}",
    url = "${rest-client.onboarding.baseUrl}")
public interface OnboardingRestClient {

  @DeleteMapping(
      value = "/idpay/onboarding/disable",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  void disableOnboarding(
      @RequestBody UnsubscribeCallDTO body);

  @PutMapping(
      value = "/idpay/onboarding/rollback/{initiativeId}/{userId}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  void rollback(
      @PathVariable String initiativeId, @PathVariable String userId);

  @PutMapping(
          value = "/idpay/onboarding/{initiativeId}/{userId}/suspend",
          produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  void suspendOnboarding(
          @PathVariable String initiativeId, @PathVariable String userId);

  @PutMapping(
          value = "/idpay/onboarding/{initiativeId}/{userId}/readmit",
          produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  void readmitOnboarding(
          @PathVariable String initiativeId, @PathVariable String userId);

}