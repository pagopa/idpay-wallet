package it.gov.pagopa.wallet.utils.zendesk;

import java.util.Locale;
import java.util.regex.Pattern;

public final class FiscalCodeUtils {
    private static final Pattern CLEANER = Pattern.compile("[^A-Z0-9]");
    private FiscalCodeUtils(){}
    public static String sanitize(String raw) {
        if (raw == null) return null;
        String up = raw.toUpperCase(Locale.ITALY);
        if (up.startsWith("TINT-")) up = up.substring(5);
        return CLEANER.matcher(up).replaceAll("");
    }
    public static boolean isValid(String cf) { return cf != null && cf.length() == 16; }
}