package it.gov.pagopa.wallet.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.IDPAYCODE_NOT_FOUND;

public class IDPayCodeNotFoundException extends ServiceException {

    public IDPayCodeNotFoundException(String message) {
        this(IDPAYCODE_NOT_FOUND, message);
    }
    public IDPayCodeNotFoundException(String message, boolean printStackTrace, Throwable ex) {
        this(IDPAYCODE_NOT_FOUND, message, null, printStackTrace, ex);
    }

    public IDPayCodeNotFoundException(String code, String message) {
        this(code, message, null, false, null);
    }

    public IDPayCodeNotFoundException(String code, String message, ServiceExceptionPayload payload, boolean printStackTrace, Throwable ex) {
        super(code, message, payload, printStackTrace, ex);
    }

}
