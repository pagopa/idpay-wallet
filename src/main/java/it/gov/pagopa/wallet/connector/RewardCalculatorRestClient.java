package it.gov.pagopa.wallet.connector;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(
    name = "${rest-client.reward-calculator.serviceCode}",
    url = "${rest-client.reward-calculator.baseUrl}")
public interface RewardCalculatorRestClient {

    @PutMapping(
      value = "/reward/onboarding/{initiativeId}/users/{userId}/counters",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  void createOnboardingCounters(
      @PathVariable String initiativeId, @PathVariable String userId);
}
