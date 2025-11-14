package it.gov.pagopa.wallet.constants;

import java.util.List;

public class WalletConstants {

  public static final String ERROR_MANDATORY_FIELD = "The field is mandatory";
  public static final String STATUS_ONBOARDING_OK = "ONBOARDING_OK";
  public static final String STATUS_JOINED = "JOINED";
  public static final String STATUS_ONBOARDING_KO = "ONBOARDING_KO";
  public static final String ONBOARDING_OPERATION = "ONBOARDING";
  public static final String TIMELINE_READMITTED = "READMITTED";
  public static final String CHANNEL_APP_IO = "IO";
  public static final String STATUS_KO = "KO";
  public static final String ERROR_MSG_HEADER_SRC_TYPE = "srcType";
  public static final String ERROR_MSG_HEADER_SRC_SERVER = "srcServer";
  public static final String ERROR_MSG_HEADER_SRC_TOPIC = "srcTopic";
  public static final String ERROR_MSG_HEADER_DESCRIPTION = "description";
  public static final String ERROR_MSG_HEADER_RETRYABLE = "retryable";
  public static final String ERROR_MSG_HEADER_RETRY = "retry";
  public static final String ERROR_MSG_HEADER_STACKTRACE = "stacktrace";
  public static final String ERROR_MSG_HEADER_CLASS = "rootCauseClass";
  public static final String ERROR_MSG_HEADER_MESSAGE = "rootCauseMessage";
  public static final String KAFKA= "kafka";

  public static final String ERROR_QUEUE= "Error to sending event to queue";
  public static final String CHANNEL_PM= "PAYMENT-MANAGER";
  public static final String ERROR_LESS_THAN_ZERO = "The field must be greater than zero!";
  public static final String REJECTED_ADD_INSTRUMENT = "REJECTED_ADD_INSTRUMENT";
  public static final String REJECTED_DELETE_INSTRUMENT = "REJECTED_DELETE_INSTRUMENT";
  public static final String INSTRUMENT_STATUS_DEFAULT = "INACTIVE";
  public static final String CHECKIBAN_KO = "CHECKIBAN_KO";
  public static final String REFUND = "REFUND";
  public static final String SUSPENSION = "SUSPENSION";
  public static final String READMISSION = "READMISSION";
  public static final String REMINDER = "REMINDER";
  public static final List<String> FILTER_INSTRUMENT_STATUS_LIST = List.of("ACTIVE", "PENDING_ENROLL_RTD",
          "PENDING_ENROLL_RE", "PENDING_DEACTIVATION_REQUEST");
  public static final String INITIATIVE_REWARD_TYPE_DISCOUNT = "DISCOUNT";
  public static final String INITIATIVE_REWARD_TYPE_REFUND = "REFUND";

  //region instrument type
  public static final String INSTRUMENT_TYPE_CARD = "CARD";
  public static final String INSTRUMENT_TYPE_QRCODE = "QRCODE";
  public static final String INSTRUMENT_TYPE_IDPAYCODE = "IDPAYCODE";
  //endregion

  public static final class ExceptionMessage {
    public static final String ERROR_SUSPENSION_STATUS_MSG = "It is not possible to suspend the user on initiative [%s]";
    public static final String ERROR_READMIT_STATUS_MSG = "It is not possible to readmit the user on initiative [%s]";

    public static final String PAYMENT_INSTRUMENT_ALREADY_ASSOCIATED_MSG = "Payment Instrument is already associated to another user";
    public static final String PAYMENT_INSTRUMENT_DELETE_NOT_ALLOWED_MSG = "It's not possible to delete an instrument of AppIO payment types";
    public static final String INITIATIVE_ENDED_MSG = "The operation is not allowed because the initiative [%s] has already ended";
    public static final String PAYMENT_INSTRUMENT_ENROLL_NOT_ALLOWED_DISCOUNT_MSG = "It is not possible to enroll a payment instrument for the discount type initiative [%s]";
    public static final String IBAN_ENROLL_NOT_ALLOWED_DISCOUNT_MSG = "It is not possible enroll an iban for the discount type initiative [%s]";
    public static final String PAYMENT_INSTRUMENT_ENROLL_NOT_ALLOWED_REFUND_MSG = "It is not possible to enroll an idpayCode for the refund type initiative [%s]";
    public static final String ERROR_UNSUBSCRIBED_INITIATIVE_MSG = "The user has unsubscribed from initiative [%s]";
    public static final String ERROR_IBAN_NOT_ITALIAN = "[%s] Iban is not italian";

    public static final String PAYMENT_INSTRUMENT_NOT_FOUND_MSG = "The selected payment instrument has not been found for the current user";
    public static final String USER_NOT_ONBOARDED_MSG = "The current user is not onboarded on initiative [%s]";
    public static final String IDPAYCODE_NOT_FOUND_MSG = "IdpayCode is not found for the current user";

    public static final String ERROR_ONBOARDING_INVOCATION_MSG = "An error occurred in the microservice onboarding";
    public static final String ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG = "An error occurred in the microservice payment instrument";
    public static final String ERROR_PAYMENT_INVOCATION_MSG = "An error occurred in the microservice payment";
  }

  public static final class ExceptionCode {
    public static final String INVALID_REQUEST = "WALLET_INVALID_REQUEST";
    public static final String SUSPENSION_NOT_ALLOWED = "WALLET_SUSPENSION_NOT_ALLOWED_FOR_USER_STATUS";
    public static final String READMISSION_NOT_ALLOWED = "WALLET_READMISSION_NOT_ALLOWED_FOR_USER_STATUS";

    public static final String ENROLL_INSTRUMENT_DISCOUNT_INITIATIVE = "WALLET_ENROLL_INSTRUMENT_NOT_ALLOWED_FOR_DISCOUNT_INITIATIVE";
    public static final String ENROLL_IBAN_DISCOUNT_INITIATIVE = "WALLET_ENROLL_IBAN_NOT_ALLOWED_FOR_DISCOUNT_INITIATIVE";
    public static final String ENROLL_INSTRUMENT_REFUND_INITIATIVE = "WALLET_ENROLL_INSTRUMENT_NOT_ALLOWED_FOR_REFUND_INITIATIVE";
    public static final String PAYMENT_INSTRUMENT_ALREADY_ASSOCIATED = "WALLET_INSTRUMENT_ALREADY_ASSOCIATED";
    public static final String PAYMENT_INSTRUMENT_DELETE_NOT_ALLOWED = "WALLET_INSTRUMENT_DELETE_NOT_ALLOWED";
    public static final String INITIATIVE_ENDED = "WALLET_INITIATIVE_ENDED";
    public static final String USER_UNSUBSCRIBED = "WALLET_USER_UNSUBSCRIBED";
    public static final String IBAN_NOT_ITALIAN = "WALLET_IBAN_NOT_ITALIAN";

    public static final String USER_NOT_ONBOARDED = "WALLET_USER_NOT_ONBOARDED";
    public static final String PAYMENT_INSTRUMENT_NOT_FOUND = "WALLET_INSTRUMENT_NOT_FOUND";
    public static final String IDPAYCODE_NOT_FOUND = "WALLET_INSTRUMENT_IDPAYCODE_NOT_FOUND";

    public static final String TOO_MANY_REQUESTS = "WALLET_TOO_MANY_REQUESTS";

    public static final String GENERIC_ERROR = "WALLET_GENERIC_ERROR";
  }

  private WalletConstants(){}
}
