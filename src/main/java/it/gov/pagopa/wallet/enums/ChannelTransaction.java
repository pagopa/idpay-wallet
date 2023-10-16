package it.gov.pagopa.wallet.enums;

import java.util.Arrays;

public enum ChannelTransaction {

    QRCODE,
    BARCODE,
    IDPAYCODE;

    public static boolean isChannelPresent(String channel) {
        return Arrays.stream(values()).anyMatch(c -> c.name().equalsIgnoreCase(channel));
    }
}

