package it.gov.pagopa.wallet.utils.zendesk;

public final class NameAliasUtils {
    private NameAliasUtils(){}
    public static String aliasFromEmail(String email) {
        if (email == null) return "user";
        int i = email.indexOf('@');
        return (i > 0) ? email.substring(0, i) : email;
    }
}
