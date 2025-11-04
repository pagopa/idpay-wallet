package it.gov.pagopa.wallet.utils.zendesk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import org.junit.jupiter.params.provider.Arguments;

class NameAliasUtilsTest {

    @ParameterizedTest
    @MethodSource("aliasCases")
    void aliasFromEmail_worksForAllCases(String input, String expected) {
        assertEquals(expected, NameAliasUtils.aliasFromEmail(input));
    }

    static Stream<Arguments> aliasCases() {
        return Stream.of(
                // null -> "user"
                arguments(null, "user"),
                // classico: prende prima della @
                arguments("mario.rossi@example.com", "mario.rossi"),
                // multiple '@' -> prende prima della PRIMA @
                arguments("john@internal@corp.com", "john"),
                // nessuna @ -> ritorna l'input com'è
                arguments("noatsymbol", "noatsymbol"),
                // '@' in prima posizione -> i=0, non >0 -> ritorna l'input com'è
                arguments("@domain.com", "@domain.com"),
                // stringa vuota -> nessuna @ -> ritorna ""
                arguments("", "")
        );
    }

    @Test
    void constructor_isPrivate_andInvocableViaReflection() throws Exception {
        Constructor<NameAliasUtils> ctor = NameAliasUtils.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(ctor.getModifiers()), "Constructor must be private");
        ctor.setAccessible(true);
        // non deve lanciare eccezioni
        ctor.newInstance();
    }
}
