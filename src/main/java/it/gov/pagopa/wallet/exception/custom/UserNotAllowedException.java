package it.gov.pagopa.wallet.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.PAYMENT_INSTRUMENT_ALREADY_ASSOCIATED;

public class UserNotAllowedException extends ServiceException {

    public UserNotAllowedException(String message, boolean printStackTrace, Throwable ex) {
        this(PAYMENT_INSTRUMENT_ALREADY_ASSOCIATED, message, null, printStackTrace, ex);
    }

    public UserNotAllowedException(String code, String message, ServiceExceptionPayload payload, boolean printStackTrace, Throwable ex) {
        super(code, message, payload, printStackTrace, ex);
    }

}
