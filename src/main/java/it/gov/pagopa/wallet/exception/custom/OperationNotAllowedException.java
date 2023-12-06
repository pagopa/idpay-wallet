package it.gov.pagopa.wallet.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;

public class OperationNotAllowedException extends ServiceException {

    public OperationNotAllowedException(String code, String message) {
        this(code, message, false, null);
    }

    public OperationNotAllowedException(String code, String message, boolean printStackTrace, Throwable ex) {
        super(code, message, printStackTrace, ex);
    }

}
