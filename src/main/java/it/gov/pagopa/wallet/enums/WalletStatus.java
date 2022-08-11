package it.gov.pagopa.wallet.enums;

import java.util.Arrays;

public enum WalletStatus {
  NOT_REFUNDABLE(false, false, false),
  NOT_REFUNDABLE_ONLY_IBAN(true, false, false),
  NOT_REFUNDABLE_ONLY_INSTRUMENT(false, true, false),
  NOT_REFUNDABLE_ONLY_EMAIL(false, false, true),
  NOT_REFUNDABLE_NO_EMAIL(true, true, false),
  NOT_REFUNDABLE_NO_INSTRUMENT(true, false, true),
  NOT_REFUNDABLE_NO_IBAN(false, true, true),
  REFUNDABLE(true, true, true);
  private final boolean hasIban;
  private final boolean hasInstrument;
  private final boolean hasEmail;

  WalletStatus(boolean hasIban, boolean hasInstrument, boolean hasEmail) {
    this.hasIban = hasIban;
    this.hasInstrument = hasInstrument;
    this.hasEmail = hasEmail;
  }

  public static WalletStatus getByBooleans(boolean hasIban, boolean hasInstrument, boolean hasEmail) {
    return Arrays.stream(values())
        .filter(
            value ->
                value.hasIban == hasIban
                    && value.hasInstrument == hasInstrument
                    && value.hasEmail == hasEmail)
        .findFirst().orElse(NOT_REFUNDABLE);
  }
}
