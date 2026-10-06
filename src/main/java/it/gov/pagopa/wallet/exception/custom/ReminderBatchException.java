package it.gov.pagopa.wallet.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.REMINDER_BATCH_FAILED;

public class ReminderBatchException extends ServiceException {

    public ReminderBatchException(String message) {
        this(REMINDER_BATCH_FAILED, message);
    }

    public ReminderBatchException(String message, boolean printStackTrace, Throwable ex) {
        this(REMINDER_BATCH_FAILED, message, null, printStackTrace, ex);
    }

    public ReminderBatchException(String code, String message) {
        this(code, message, null, false, null);
    }

    public ReminderBatchException(String code, String message, ServiceExceptionPayload payload, boolean printStackTrace, Throwable ex) {
        super(code, message, payload, printStackTrace, ex);
    }

}

