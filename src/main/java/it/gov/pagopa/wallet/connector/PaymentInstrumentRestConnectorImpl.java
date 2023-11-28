package it.gov.pagopa.wallet.connector;

import feign.FeignException;
import it.gov.pagopa.wallet.dto.*;
import it.gov.pagopa.wallet.exception.custom.*;
import org.springframework.stereotype.Service;

import java.util.List;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionMessage.*;

@Service
public class PaymentInstrumentRestConnectorImpl implements PaymentInstrumentRestConnector {

  private final PaymentInstrumentRestClient paymentInstrumentRestClient;

  public PaymentInstrumentRestConnectorImpl(
      PaymentInstrumentRestClient paymentInstrumentRestClient) {
    this.paymentInstrumentRestClient = paymentInstrumentRestClient;
  }

  @Override
  public void enrollInstrument(InstrumentCallBodyDTO body) {
    try {
      paymentInstrumentRestClient.enrollInstrument(body);
    } catch (FeignException e){
      if (e.status() == 403){
        throw new UserNotAllowedException(PAYMENT_INSTRUMENT_ALREADY_ASSOCIATED_MSG);
      }

      if (e.status() == 404){
        throw new PaymentInstrumentNotFoundException(PAYMENT_INSTRUMENT_NOT_FOUND_MSG);
      }

      throw new PaymentInstrumentInvocationException(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG);
    }
  }

  @Override
  public void disableAllInstrument(UnsubscribeCallDTO body) {
    try {
      paymentInstrumentRestClient.disableAllInstrument(body);
    } catch (FeignException e){
      throw new PaymentInstrumentInvocationException(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG);
    }
  }

  @Override
  public void deleteInstrument(DeactivationBodyDTO body) {
    try {
      paymentInstrumentRestClient.deleteInstrument(body);
    } catch (FeignException e){
      if (e.status() == 403){
        throw new InstrumentDeleteNotAllowedException(PAYMENT_INSTRUMENT_DELETE_NOT_ALLOWED_MSG);
      }
      if (e.status() == 404){
        throw new PaymentInstrumentNotFoundException(PAYMENT_INSTRUMENT_NOT_FOUND_MSG);
      }

      throw new PaymentInstrumentInvocationException(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG);
    }
  }

  @Override
  public void enrollInstrumentIssuer(InstrumentIssuerCallDTO body) {
    try {
      paymentInstrumentRestClient.enrollInstrumentIssuer(body);
    } catch (FeignException e){
      if (e.status() == 403){
        throw new UserNotAllowedException(PAYMENT_INSTRUMENT_ALREADY_ASSOCIATED_MSG);
      }

      throw new PaymentInstrumentInvocationException(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG);
    }
  }
  @Override
  public InstrumentDetailDTO getInstrumentInitiativesDetail(String idWallet, String userId, List<String> statusList){
    try {
      return paymentInstrumentRestClient.getInstrumentInitiativesDetail(idWallet, userId, statusList);
    } catch (FeignException e){
      if (e.status() == 404){
        throw new PaymentInstrumentNotFoundException(PAYMENT_INSTRUMENT_NOT_FOUND_MSG);
      }

      throw new PaymentInstrumentInvocationException(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG);
    }
  }

  @Override
  public void enrollDiscountInitiative(InstrumentFromDiscountDTO body) {
    try {
      paymentInstrumentRestClient.enrollDiscountInitiative(body);
    } catch (FeignException e){
      throw new PaymentInstrumentInvocationException(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG);
    }
  }

  @Override
  public void rollback(String initiativeId, String userId) {
    try {
      paymentInstrumentRestClient.rollback(initiativeId, userId);
    } catch (FeignException e){
      throw new PaymentInstrumentInvocationException(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG);
    }
  }

  @Override
  public void enrollInstrumentCode(InstrumentCallBodyDTO body) {
    try {
      paymentInstrumentRestClient.enrollInstrumentCode(body);
    } catch (FeignException e){
      if (e.status() == 403){
        throw new IdPayCodeNotEnabledException(IDPAYCODE_NOT_GENERATED_MSG);
      }

      throw new PaymentInstrumentInvocationException(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG);
    }
  }
}
