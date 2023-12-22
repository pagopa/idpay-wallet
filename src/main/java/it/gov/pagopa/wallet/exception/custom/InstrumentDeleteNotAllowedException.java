package it.gov.pagopa.wallet.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.PAYMENT_INSTRUMENT_DELETE_NOT_ALLOWED;

public class InstrumentDeleteNotAllowedException extends ServiceException {

    public InstrumentDeleteNotAllowedException(String message, boolean printStackTrace, Throwable ex) {
        this(PAYMENT_INSTRUMENT_DELETE_NOT_ALLOWED, message, null, printStackTrace, ex);
    }

    public InstrumentDeleteNotAllowedException(String code, String message, ServiceExceptionPayload payload, boolean printStackTrace, Throwable ex) {
        super(code, message, payload, printStackTrace, ex);
    }

}
