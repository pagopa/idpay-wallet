package it.gov.pagopa.wallet.utils.zendesk;

import java.util.Locale;
import java.util.regex.Pattern;

public final class FiscalCodeUtils {
    private static final Pattern CLEANER = Pattern.compile("[^A-Z0-9]");
    private FiscalCodeUtils(){}

    public static String sanitize(String raw) {
        if (raw == null) return null;
        String up = raw.toUpperCase(Locale.ITALY).trim();
        if (up.startsWith("TINIT-")) up = up.substring("TINIT-".length());
        if (up.startsWith("TINT-"))  up = up.substring("TINT-".length()); // compat
        up = CLEANER.matcher(up).replaceAll("");
        // se arrivasse oltre 16, ritaglio a 16 (il CF italiano è 16)
        return (up.length() > 16) ? up.substring(0, 16) : up;
    }

    public static boolean isValid(String cf) { return cf != null && cf.length() == 16; }
}
