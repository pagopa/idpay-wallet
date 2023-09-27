package it.gov.pagopa.wallet.connector;

import it.gov.pagopa.wallet.dto.DeactivationBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentCallBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentDetailDTO;
import it.gov.pagopa.wallet.dto.InstrumentFromDiscountDTO;
import it.gov.pagopa.wallet.dto.InstrumentIssuerCallDTO;
import it.gov.pagopa.wallet.dto.UnsubscribeCallDTO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(
    name = "${rest-client.payment.instrument.serviceCode}",
    url = "${rest-client.payment.instrument.baseUrl}")
public interface PaymentInstrumentRestClient {

  @PutMapping(value = "/idpay/instrument/enroll", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  void enrollInstrument(@RequestBody InstrumentCallBodyDTO body);

  @DeleteMapping(value = "/idpay/instrument/disableall")
  @ResponseBody
  void disableAllInstrument(@RequestBody UnsubscribeCallDTO body);

  @DeleteMapping(
      value = "/idpay/instrument/deactivate",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  void deleteInstrument(@RequestBody DeactivationBodyDTO body);

  @PutMapping(value = "/idpay/instrument/hb/enroll", produces = MediaType.APPLICATION_JSON_VALUE)
  void enrollInstrumentIssuer(InstrumentIssuerCallDTO body);

  @GetMapping(
      value = "/idpay/instrument/initiatives/{idWallet}/{userId}/detail",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  InstrumentDetailDTO getInstrumentInitiativesDetail(
      @PathVariable("idWallet") String idWallet,
      @PathVariable("userId") String userId,
      @RequestParam(required = false) List<String> statusList);

  @PutMapping(value = "/idpay/instrument/discount/enroll", produces = MediaType.APPLICATION_JSON_VALUE)
  void enrollDiscountInitiative(@RequestBody InstrumentFromDiscountDTO body);

  @PutMapping(
          value = "/idpay/instrument/rollback/{initiativeId}/{userId}",
          produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  void rollback(
          @PathVariable String initiativeId, @PathVariable String userId);

  @PutMapping(value = "/idpay/instrument/code/enroll", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  void enrollInstrumentCode(@RequestBody InstrumentCallBodyDTO body);

}
