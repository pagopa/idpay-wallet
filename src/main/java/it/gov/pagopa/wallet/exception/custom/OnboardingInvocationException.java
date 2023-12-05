package it.gov.pagopa.wallet.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.GENERIC_ERROR;

public class OnboardingInvocationException extends ServiceException {

    public OnboardingInvocationException(String message) {
        this(GENERIC_ERROR, message);
    }

    public OnboardingInvocationException(String code, String message) {
        this(code, message, false, null);
    }

    public OnboardingInvocationException(String code, String message, boolean printStackTrace, Throwable ex) {
        super(code, message, printStackTrace, ex);
    }

}
