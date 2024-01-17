package it.gov.pagopa.wallet.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.USER_NOT_ONBOARDED;

public class UserNotOnboardedException extends ServiceException {

    public UserNotOnboardedException(String message) {
        this(USER_NOT_ONBOARDED, message);
    }

    public UserNotOnboardedException(String message, boolean printStackTrace, Throwable ex) {
        this(USER_NOT_ONBOARDED, message, null, printStackTrace, ex);
    }

    public UserNotOnboardedException(String code, String message) {
        this(code, message, null, false, null);
    }

    public UserNotOnboardedException(String code, String message, ServiceExceptionPayload payload, boolean printStackTrace, Throwable ex) {
        super(code, message, payload, printStackTrace, ex);
    }

}
