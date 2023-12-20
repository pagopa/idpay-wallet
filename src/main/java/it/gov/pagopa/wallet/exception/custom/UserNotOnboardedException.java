package it.gov.pagopa.wallet.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionResponse;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.USER_NOT_ONBOARDED;

public class UserNotOnboardedException extends ServiceException {

    public UserNotOnboardedException(String message) {
        this(USER_NOT_ONBOARDED, message);
    }

    public UserNotOnboardedException(String code, String message) {
        this(code, message, null, false, null);
    }

    public UserNotOnboardedException(String code, String message, ServiceExceptionResponse response, boolean printStackTrace, Throwable ex) {
        super(code, message, response, printStackTrace, ex);
    }

}
