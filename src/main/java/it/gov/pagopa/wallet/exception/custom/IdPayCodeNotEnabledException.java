package it.gov.pagopa.wallet.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.IDPAYCODE_NOT_GENERATED;

public class IdPayCodeNotEnabledException extends ServiceException {

    public IdPayCodeNotEnabledException(String message) {
        this(IDPAYCODE_NOT_GENERATED, message);
    }

    public IdPayCodeNotEnabledException(String code, String message) {
        this(code, message, false, null);
    }

    public IdPayCodeNotEnabledException(String code, String message, boolean printStackTrace, Throwable ex) {
        super(code, message, printStackTrace, ex);
    }

}
