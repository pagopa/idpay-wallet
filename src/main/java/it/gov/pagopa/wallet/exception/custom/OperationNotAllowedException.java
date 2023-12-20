package it.gov.pagopa.wallet.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionResponse;

public class OperationNotAllowedException extends ServiceException {

    public OperationNotAllowedException(String code, String message) {
        this(code, message, null, false, null);
    }

    public OperationNotAllowedException(String code, String message, ServiceExceptionResponse response, boolean printStackTrace, Throwable ex) {
        super(code, message, response, printStackTrace, ex);
    }

}
