package it.gov.pagopa.wallet.connector;

import it.gov.pagopa.wallet.dto.InstrumentCallBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(
    name = "${rest-client.payment.intrument.serviceCode}",
    url = "${payment.instrument.uri}")
public interface PaymentInstrumentRestClient {

  @PutMapping(
      value = "/idpay/instrument/enroll",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  InstrumentResponseDTO enrollInstrument(
      @RequestBody InstrumentCallBodyDTO body);

}