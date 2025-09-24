package it.gov.pagopa.wallet.connector;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import it.gov.pagopa.wallet.dto.payment.TransactionBarCodeCreationRequest;
import it.gov.pagopa.wallet.dto.payment.TransactionBarCodeEnrichedResponse;
import it.gov.pagopa.wallet.exception.custom.InitiativeInvalidException;
import it.gov.pagopa.wallet.exception.custom.PaymentInvocationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PaymentRestConnectorImplTest {

    private PaymentRestClient paymentRestClient;
    private PaymentRestConnectorImpl connector;

    @BeforeEach
    void setUp() {
        paymentRestClient = Mockito.mock(PaymentRestClient.class);
        connector = new PaymentRestConnectorImpl(paymentRestClient);
    }

    @Test
    void createExtendedTransaction_ok() {
        TransactionBarCodeCreationRequest request = new TransactionBarCodeCreationRequest();
        TransactionBarCodeEnrichedResponse expected = new TransactionBarCodeEnrichedResponse();
        when(paymentRestClient.createExtendedTransaction(any(), eq("user1")))
                .thenReturn(expected);

        TransactionBarCodeEnrichedResponse result =
                connector.createExtendedTransaction(request, "user1");

        assertThat(result).isSameAs(expected);
        verify(paymentRestClient).createExtendedTransaction(request, "user1");
    }

    @Test
    void createExtendedTransaction_403_throwsInitiativeInvalidException() {
        TransactionBarCodeCreationRequest request = new TransactionBarCodeCreationRequest();
        request.setInitiativeId("INIT123");

        // costruiamo un FeignException con status 403
        FeignException forbidden = new FeignException.Forbidden(
                "forbidden",
                Request.create(Request.HttpMethod.GET, "/url", Collections.emptyMap(), null,
                        StandardCharsets.UTF_8, new RequestTemplate()),
                null,
                Collections.emptyMap());

        when(paymentRestClient.createExtendedTransaction(any(), any()))
                .thenThrow(forbidden);

        assertThatThrownBy(() -> connector.createExtendedTransaction(request, "user42"))
                .isInstanceOf(InitiativeInvalidException.class)
                .satisfies(ex -> {
                    InitiativeInvalidException iie = (InitiativeInvalidException) ex;
                    // verifichiamo i campi base della ServiceException
                    assertThat(iie.getCode()).isEqualTo("WALLET_INITIATIVE_ENDED");
                    assertThat(iie.getMessage()).contains("INIT123");
                    assertThat(iie.getCause()).isSameAs(forbidden);
                });
    }

    @Test
    void createExtendedTransaction_otherError_throwsPaymentInvocationException() {
        TransactionBarCodeCreationRequest request = new TransactionBarCodeCreationRequest();

        // FeignException con status diverso da 403
        FeignException badRequest = new FeignException.BadRequest(
                "bad request",
                Request.create(Request.HttpMethod.GET, "/url", Collections.emptyMap(), null,
                        StandardCharsets.UTF_8, new RequestTemplate()),
                null,
                Collections.emptyMap());

        when(paymentRestClient.createExtendedTransaction(any(), any()))
                .thenThrow(badRequest);

        assertThatThrownBy(() -> connector.createExtendedTransaction(request, "user99"))
                .isInstanceOf(PaymentInvocationException.class)
                .satisfies(ex -> {
                    PaymentInvocationException pie = (PaymentInvocationException) ex;
                    // i costruttori della classe PaymentInvocationException sono così testati
                    assertThat(pie.getCode()).isEqualTo("WALLET_GENERIC_ERROR");
                    assertThat(pie.getMessage()).contains("payment"); // messaggio da costante
                    assertThat(pie.getCause()).isSameAs(badRequest);
                });
    }
}

