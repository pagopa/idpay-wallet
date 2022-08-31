package it.gov.pagopa.wallet.constants;

public class WalletConstants {

  public static final String ERROR_MANDATORY_FIELD = "The field is mandatory!";
  public static final String ERROR_INITIATIVE_KO = "The requested initiative is not active!";

  public static final String ERROR_INITIATIVE_UNSUBSCRIBED = "You are unsubscribed at this initiative!";

  public static final String ERROR_WALLET_NOT_FOUND = "The requested initiative is not active for the current user!";

  public static final String STATUS_NOT_REFUNDABLE = "NOT_REFUNDABLE";
  public static final String STATUS_NOT_REFUNDABLE_ONLY_IBAN = "NOT_REFUNDABLE_ONLY_IBAN";
  public static final String STATUS_NOT_REFUNDABLE_ONLY_INSTRUMENT = "NOT_REFUNDABLE_ONLY_INSTRUMENT";
  public static final String STATUS_REFUNDABLE = "REFUNDABLE";

  public static final String STATUS_ONBOARDING_OK = "ONBOARDING_OK";

  public static final String ONBOARDING_OPERATION = "ONBOARDING";
  public static final String CHANNEL_APP_IO = "APP_IO";
  public static final String HOLDER_BANK = "Unicredit";
  public static final String STATUS_KO = "KO";
  public static final String ERROR_EMAIL_NOT_VALID = "Please provide a valid email address!";


  private WalletConstants(){}
}
