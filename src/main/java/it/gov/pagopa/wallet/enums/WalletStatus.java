package it.gov.pagopa.wallet.enums;

import java.util.Arrays;

public enum WalletStatus {
  NOT_REFUNDABLE(false, false),
  NOT_REFUNDABLE_ONLY_IBAN(true, false),
  NOT_REFUNDABLE_ONLY_INSTRUMENT(false, true),
  REFUNDABLE(true, true);
  private final boolean hasIban;
  private final boolean hasInstrument;

  WalletStatus(boolean hasIban, boolean hasInstrument) {
    this.hasIban = hasIban;
    this.hasInstrument = hasInstrument;
  }

  public static WalletStatus getByBooleans(boolean hasIban, boolean hasInstrument) {
    return Arrays.stream(values())
        .filter(
            value ->
                value.hasIban == hasIban
                    && value.hasInstrument == hasInstrument)
        .findFirst().orElse(NOT_REFUNDABLE);
  }
}
