package it.gov.pagopa.wallet.constants;

import java.util.List;

public class WalletConstants {

  public static final String ERROR_MANDATORY_FIELD = "The field is mandatory!";
  public static final String ERROR_INITIATIVE_KO = "The requested initiative is not active!";

  public static final String ERROR_INITIATIVE_UNSUBSCRIBED = "You are unsubscribed at this initiative!";

  public static final String ERROR_WALLET_NOT_FOUND = "The requested initiative is not active for the current user!";
  public static final String STATUS_ONBOARDING_OK = "ONBOARDING_OK";
  public static final String STATUS_JOINED = "JOINED";
  public static final String STATUS_ONBOARDING_KO = "ONBOARDING_KO";
  public static final String ONBOARDING_OPERATION = "ONBOARDING";
  public static final String TIMELINE_READMITTED = "READMITTED";
  public static final String CHANNEL_APP_IO = "APP_IO";
  public static final String STATUS_KO = "KO";
  public static final String ERROR_MSG_HEADER_SRC_TYPE = "srcType";
  public static final String ERROR_MSG_HEADER_SRC_SERVER = "srcServer";
  public static final String ERROR_MSG_HEADER_SRC_TOPIC = "srcTopic";
  public static final String ERROR_MSG_HEADER_DESCRIPTION = "description";
  public static final String ERROR_MSG_HEADER_RETRYABLE = "retryable";
  public static final String ERROR_MSG_HEADER_STACKTRACE = "stacktrace";
  public static final String ERROR_MSG_HEADER_CLASS = "rootCauseClass";
  public static final String ERROR_MSG_HEADER_MESSAGE = "rootCauseMessage";
  public static final String KAFKA= "kafka";

  public static final String ERROR_QUEUE= "Error to sending event to queue";
  public static final String CHANNEL_PM= "PAYMENT-MANAGER";
  public static final String ERROR_LESS_THAN_ZERO = "The field must be greater than zero!";
  public static final String REJECTED_ADD_INSTRUMENT = "REJECTED_ADD_INSTRUMENT";
  public static final String ADD_INSTRUMENT = "ADD_INSTRUMENT";
  public static final String INSTRUMENT_STATUS_DEFAULT = "INACTIVE";
  public static final String CHECKIBAN_KO = "CHECKIBAN_KO";
  public static final String REFUND = "REFUND";
  public static final String SUSPENSION = "SUSPENSION";
  public static final String READMISSION = "READMISSION";
  public static final List<String> FILTER_INSTRUMENT_STATUS_LIST = List.of("ACTIVE", "PENDING_ENROLL_RTD",
          "PENDING_ENROLL_RE", "PENDING_DEACTIVATION_REQUEST");
  public static final String INITIATIVE_REWARD_TYPE_DISCOUNT = "DISCOUNT";
  public static final String INITIATIVE_REWARD_TYPE_REFUND = "REFUND";
  public static final String ERROR_INITIATIVE_DISCOUNT_PI = "It is not possible enroll a payment instrument for a discount type initiative";
  public static final String ERROR_INITIATIVE_DISCOUNT_IBAN = "It is not possible enroll an iban for a discount type initiative";

  //region instrument type
  public static final String INSTRUMENT_TYPE_CARD = "CARD";
  public static final String INSTRUMENT_TYPE_QRCODE = "QRCODE";
  public static final String INSTRUMENT_TYPE_IDPAYCODE = "IDPAYCODE";
  //endregion
  private WalletConstants(){}
}
