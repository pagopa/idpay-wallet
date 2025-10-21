package it.gov.pagopa.wallet.connector;

import it.gov.pagopa.wallet.dto.payment.TransactionBarCodeCreationRequest;
import it.gov.pagopa.wallet.dto.payment.TransactionBarCodeEnrichedResponse;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@FeignClient(
    name = "${rest-client.payment.serviceCode}",
    url = "${rest-client.payment.baseUrl}")
public interface PaymentRestClient {

    @PostMapping(
            value    = "/idpay/payment/bar-code/extended",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    TransactionBarCodeEnrichedResponse createExtendedTransaction(
            @RequestBody @Valid TransactionBarCodeCreationRequest trxBarCodeCreationRequest,
            @RequestHeader("x-user-id") String userId
    );
}
