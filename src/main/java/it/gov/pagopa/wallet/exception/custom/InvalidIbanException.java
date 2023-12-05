package it.gov.pagopa.wallet.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.IBAN_NOT_ITALIAN;

public class InvalidIbanException extends ServiceException {

    public InvalidIbanException(String message) {
        this(IBAN_NOT_ITALIAN, message);
    }

    public InvalidIbanException(String code, String message) {
        this(code, message, false, null);
    }

    public InvalidIbanException(String code, String message, boolean printStackTrace, Throwable ex) {
        super(code, message, printStackTrace, ex);
    }

}
