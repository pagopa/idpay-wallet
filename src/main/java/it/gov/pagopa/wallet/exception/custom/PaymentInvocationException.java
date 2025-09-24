package it.gov.pagopa.wallet.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.GENERIC_ERROR;

public class PaymentInvocationException extends ServiceException {

    public PaymentInvocationException(String message) {
        this(GENERIC_ERROR, message);
    }
    public PaymentInvocationException(String message, boolean printStackTrace, Throwable ex) {
        this(GENERIC_ERROR, message, null, printStackTrace, ex);
    }

    public PaymentInvocationException(String code, String message) {
        this(code, message, null, false, null);
    }

    public PaymentInvocationException(String code, String message, ServiceExceptionPayload payload, boolean printStackTrace, Throwable ex) {
        super(code, message, payload, printStackTrace, ex);
    }

}
