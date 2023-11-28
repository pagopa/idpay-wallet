package it.gov.pagopa.wallet.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.PAYMENT_INSTRUMENT_NOT_FOUND;

public class PaymentInstrumentNotFoundException extends ServiceException {

    public PaymentInstrumentNotFoundException(String message) {
        this(PAYMENT_INSTRUMENT_NOT_FOUND, message);
    }

    public PaymentInstrumentNotFoundException(String code, String message) {
        this(code, message, false, null);
    }

    public PaymentInstrumentNotFoundException(String code, String message, boolean printStackTrace, Throwable ex) {
        super(code, message, printStackTrace, ex);
    }

}
