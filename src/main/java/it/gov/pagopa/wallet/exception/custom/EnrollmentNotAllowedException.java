package it.gov.pagopa.wallet.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

public class EnrollmentNotAllowedException extends ServiceException {

    public EnrollmentNotAllowedException(String code, String message) {
        this(code, message, null, false, null);
    }

    public EnrollmentNotAllowedException(String code, String message, ServiceExceptionPayload payload, boolean printStackTrace, Throwable ex) {
        super(code, message, payload, printStackTrace, ex);
    }

}
