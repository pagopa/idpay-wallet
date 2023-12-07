package it.gov.pagopa.wallet.connector;

import feign.FeignException;
import it.gov.pagopa.wallet.dto.*;
import it.gov.pagopa.wallet.exception.custom.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionMessage.*;

@Service
@Slf4j
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
        log.info("[ENROLL_INSTRUMENT] The payment instrument with idWallet {} is already associated with another user",
                body.getIdWallet());
        throw new UserNotAllowedException(PAYMENT_INSTRUMENT_ALREADY_ASSOCIATED_MSG);
      }

      if (e.status() == 404){
        log.info("[ENROLL_INSTRUMENT] The payment instrument with idWallet {} has not been found for user {}",
                body.getIdWallet(), body.getUserId());
        throw new PaymentInstrumentNotFoundException(PAYMENT_INSTRUMENT_NOT_FOUND_MSG);
      }

      log.error("[ENROLL_INSTRUMENT] An error occurred while invoking the payment.instrument microservice");
      throw new PaymentInstrumentInvocationException(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG);
    }
  }

  @Override
  public void disableAllInstrument(UnsubscribeCallDTO body) {
    try {
      paymentInstrumentRestClient.disableAllInstrument(body);
    } catch (FeignException e){
      log.error("[DISABLE_ALL_INSTRUMENT] An error occurred while invoking the payment instrument microservice");
      throw new PaymentInstrumentInvocationException(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG);
    }
  }

  @Override
  public void deleteInstrument(DeactivationBodyDTO body) {
    try {
      paymentInstrumentRestClient.deleteInstrument(body);
    } catch (FeignException e){
      if (e.status() == 403){
        log.info("[DELETE_INSTRUMENT] It is not possible to delete the instrument with id {} of AppIO payment type",
                body.getInstrumentId());
        throw new InstrumentDeleteNotAllowedException(PAYMENT_INSTRUMENT_DELETE_NOT_ALLOWED_MSG);
      }
      if (e.status() == 404){
        log.info("[DELETE_INSTRUMENT] The payment instrument with id {} has not been found for user {}",
                body.getInstrumentId(), body.getUserId());
        throw new PaymentInstrumentNotFoundException(PAYMENT_INSTRUMENT_NOT_FOUND_MSG);
      }

      log.error("[DELETE_INSTRUMENT] An error occurred while invoking the payment instrument microservice");
      throw new PaymentInstrumentInvocationException(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG);
    }
  }

  @Override
  public void enrollInstrumentIssuer(InstrumentIssuerCallDTO body) {
    try {
      paymentInstrumentRestClient.enrollInstrumentIssuer(body);
    } catch (FeignException e){
      if (e.status() == 403){
        log.info("[ENROLL_INSTRUMENT_ISSUER] The payment instrument with maskedPan {} is already associated with another user",
                body.getMaskedPan());
        throw new UserNotAllowedException(PAYMENT_INSTRUMENT_ALREADY_ASSOCIATED_MSG);
      }

      log.error("[ENROLL_INSTRUMENT_ISSUER] An error occurred while invoking the payment instrument microservice");
      throw new PaymentInstrumentInvocationException(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG);
    }
  }
  @Override
  public InstrumentDetailDTO getInstrumentInitiativesDetail(String idWallet, String userId, List<String> statusList){
    try {
      return paymentInstrumentRestClient.getInstrumentInitiativesDetail(idWallet, userId, statusList);
    } catch (FeignException e){
      if (e.status() == 404){
        log.info("[GET_INSTRUMENT_INITIATIVE_DETAIL] The payment instrument with idWallet {} has not been found for user {}",
                idWallet, userId);
        throw new PaymentInstrumentNotFoundException(PAYMENT_INSTRUMENT_NOT_FOUND_MSG);
      }

      log.error("[GET_INSTRUMENT_INITIATIVE_DETAIL] An error occurred while invoking the payment instrument microservice");
      throw new PaymentInstrumentInvocationException(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG);
    }
  }

  @Override
  public void enrollDiscountInitiative(InstrumentFromDiscountDTO body) {
    try {
      paymentInstrumentRestClient.enrollDiscountInitiative(body);
    } catch (FeignException e){
      log.error("[ENROLL_DISCOUNT_INITIATIVE] An error occurred while invoking the payment instrument microservice");
      throw new PaymentInstrumentInvocationException(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG);
    }
  }

  @Override
  public void rollback(String initiativeId, String userId) {
    try {
      paymentInstrumentRestClient.rollback(initiativeId, userId);
    } catch (FeignException e){
      log.error("[ROLLBACK] An error occurred while invoking the payment instrument microservice");
      throw new PaymentInstrumentInvocationException(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG);
    }
  }

  @Override
  public void enrollInstrumentCode(InstrumentCallBodyDTO body) {
    try {
      paymentInstrumentRestClient.enrollInstrumentCode(body);
    } catch (FeignException e){
      if (e.status() == 404){
        log.info("[ENROLL_INSTRUMENT_CODE] The idpayCode with idWallet {} has not been found for user {}",
                body.getIdWallet(), body.getUserId());
        throw new IDPayCodeNotFoundException(IDPAYCODE_NOT_FOUND_MSG);
      }

      log.error("[ENROLL_INSTRUMENT_CODE] An error occurred while invoking the payment instrument microservice");
      throw new PaymentInstrumentInvocationException(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG);
    }
  }
}
