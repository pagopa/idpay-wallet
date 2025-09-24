package it.gov.pagopa.wallet.connector;

import it.gov.pagopa.wallet.dto.payment.TransactionBarCodeCreationRequest;
import it.gov.pagopa.wallet.dto.payment.TransactionBarCodeEnrichedResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

public interface PaymentRestConnector {
    TransactionBarCodeEnrichedResponse createExtendedTransaction(
            @RequestBody @Valid TransactionBarCodeCreationRequest trxBarCodeCreationRequest,
            @RequestHeader("x-user-id") String userId
    );
}
