package it.gov.pagopa.wallet.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionResponse;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.IDPAYCODE_NOT_FOUND;

public class IDPayCodeNotFoundException extends ServiceException {

    public IDPayCodeNotFoundException(String message) {
        this(IDPAYCODE_NOT_FOUND, message);
    }

    public IDPayCodeNotFoundException(String code, String message) {
        this(code, message, null, false, null);
    }

    public IDPayCodeNotFoundException(String code, String message, ServiceExceptionResponse response, boolean printStackTrace, Throwable ex) {
        super(code, message, response, printStackTrace, ex);
    }

}
