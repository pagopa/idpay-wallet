package it.gov.pagopa.wallet.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.PAYMENT_INSTRUMENT_NOT_FOUND;

public class PaymentInstrumentNotFoundException extends ServiceException {

    public PaymentInstrumentNotFoundException(String message) {
        this(PAYMENT_INSTRUMENT_NOT_FOUND, message);
    }
    public PaymentInstrumentNotFoundException(String message, boolean printStackTrace, Throwable ex) {
        this(PAYMENT_INSTRUMENT_NOT_FOUND, message, null, printStackTrace, ex);
    }

    public PaymentInstrumentNotFoundException(String code, String message) {
        this(code, message, null, false, null);
    }

    public PaymentInstrumentNotFoundException(String code, String message, ServiceExceptionPayload payload, boolean printStackTrace, Throwable ex) {
        super(code, message, payload, printStackTrace, ex);
    }

}
