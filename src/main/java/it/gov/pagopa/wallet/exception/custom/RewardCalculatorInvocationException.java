package it.gov.pagopa.wallet.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.GENERIC_ERROR;

public class RewardCalculatorInvocationException extends ServiceException {

  public RewardCalculatorInvocationException(String message) {
    this(GENERIC_ERROR, message);
  }

  public RewardCalculatorInvocationException(String message, boolean printStackTrace,
      Throwable ex) {
    this(GENERIC_ERROR, message, null, printStackTrace, ex);
  }

  public RewardCalculatorInvocationException(String code, String message) {
    this(code, message, null, false, null);
  }

  public RewardCalculatorInvocationException(String code, String message,
      ServiceExceptionPayload payload, boolean printStackTrace, Throwable ex) {
    super(code, message, payload, printStackTrace, ex);
  }
}
