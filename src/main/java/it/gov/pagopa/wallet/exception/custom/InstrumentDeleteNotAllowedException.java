package it.gov.pagopa.wallet.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionResponse;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.PAYMENT_INSTRUMENT_DELETE_NOT_ALLOWED;

public class InstrumentDeleteNotAllowedException extends ServiceException {

    public InstrumentDeleteNotAllowedException(String message) {
        this(PAYMENT_INSTRUMENT_DELETE_NOT_ALLOWED, message);
    }

    public InstrumentDeleteNotAllowedException(String code, String message) {
        this(code, message, null, false, null);
    }

    public InstrumentDeleteNotAllowedException(String code, String message, ServiceExceptionResponse response, boolean printStackTrace, Throwable ex) {
        super(code, message, response, printStackTrace, ex);
    }

}
