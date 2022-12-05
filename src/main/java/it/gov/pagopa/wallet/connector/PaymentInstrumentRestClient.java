package it.gov.pagopa.wallet.connector;

import it.gov.pagopa.wallet.dto.DeactivationBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentCallBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentIssuerDTO;
import it.gov.pagopa.wallet.dto.UnsubscribeCallDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(
    name = "${rest-client.payment.instrument.serviceCode}",
    url = "${rest-client.payment.instrument.baseUrl}")
public interface PaymentInstrumentRestClient {

  @PutMapping(
      value = "/idpay/instrument/enroll",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  void enrollInstrument(
      @RequestBody InstrumentCallBodyDTO body);

  @DeleteMapping(
      value = "/idpay/instrument/disableall")
  @ResponseBody
  void disableAllInstrument(
      @RequestBody UnsubscribeCallDTO body);

@DeleteMapping(
      value = "/idpay/instrument/deactivate",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
void deleteInstrument(
      @RequestBody DeactivationBodyDTO body);

  @PutMapping(
      value = "/idpay/instrument/hb/enroll",
      produces = MediaType.APPLICATION_JSON_VALUE)
  void enrollInstrumentIssuer(InstrumentIssuerDTO body);
}