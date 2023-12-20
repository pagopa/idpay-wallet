package it.gov.pagopa.wallet.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionResponse;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.PAYMENT_INSTRUMENT_ALREADY_ASSOCIATED;

public class UserNotAllowedException extends ServiceException {

    public UserNotAllowedException(String message) {
        this(PAYMENT_INSTRUMENT_ALREADY_ASSOCIATED, message);
    }

    public UserNotAllowedException(String code, String message) {
        this(code, message, null, false, null);
    }

    public UserNotAllowedException(String code, String message, ServiceExceptionResponse response, boolean printStackTrace, Throwable ex) {
        super(code, message, response, printStackTrace, ex);
    }

}
