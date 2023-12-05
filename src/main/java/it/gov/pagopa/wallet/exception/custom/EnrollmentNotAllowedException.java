package it.gov.pagopa.wallet.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;

public class EnrollmentNotAllowedException extends ServiceException {

    public EnrollmentNotAllowedException(String code, String message) {
        this(code, message, false, null);
    }

    public EnrollmentNotAllowedException(String code, String message, boolean printStackTrace, Throwable ex) {
        super(code, message, printStackTrace, ex);
    }

}
