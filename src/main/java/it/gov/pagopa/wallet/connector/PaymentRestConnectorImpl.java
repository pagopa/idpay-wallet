package it.gov.pagopa.wallet.connector;

import feign.FeignException;
import it.gov.pagopa.wallet.dto.*;
import it.gov.pagopa.wallet.dto.payment.TransactionBarCodeCreationRequest;
import it.gov.pagopa.wallet.dto.payment.TransactionBarCodeEnrichedResponse;
import it.gov.pagopa.wallet.exception.custom.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.INITIATIVE_ENDED;
import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionMessage.*;

@Service
@Slf4j
public class PaymentRestConnectorImpl implements PaymentRestConnector {

  private final PaymentRestClient paymentRestClient;

  public PaymentRestConnectorImpl(
          PaymentRestClient paymentRestClient) {
    this.paymentRestClient = paymentRestClient;
  }

    @Override
    public TransactionBarCodeEnrichedResponse createExtendedTransaction(TransactionBarCodeCreationRequest trxBarCodeCreationRequest, String userId) {
        String sanitizedInitiativeId = sanitizeString(trxBarCodeCreationRequest.getInitiativeId());
        String sanitizedUserId = sanitizeString(userId);
      try{
          return paymentRestClient.createExtendedTransaction(trxBarCodeCreationRequest, userId);
      } catch (FeignException e){
          if (e.status() == 403){
              log.error("[CREATE_EXTENDED_TRANSACTION] Voucher generation for the user {} received an error as the initiative {} has ended", sanitizedUserId, sanitizedInitiativeId);
              throw new InitiativeInvalidException(INITIATIVE_ENDED, String.format(INITIATIVE_ENDED_MSG, trxBarCodeCreationRequest.getInitiativeId()), null, true, e);
          }

          log.error("[CREATE_EXTENDED_TRANSACTION] An error occurred while invoking the payment microservice");
          throw new PaymentInvocationException(ERROR_PAYMENT_INVOCATION_MSG, true, e);
      }
    }

    public static String sanitizeString(String str) {
        return str == null ? null : str.replaceAll("[\\r\\n]", "").replaceAll("[^\\w\\s-]", "");
    }
}
