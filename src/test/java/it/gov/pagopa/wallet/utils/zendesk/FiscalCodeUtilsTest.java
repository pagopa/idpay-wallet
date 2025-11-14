package it.gov.pagopa.wallet.utils.zendesk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import static it.gov.pagopa.wallet.utils.zendesk.FiscalCodeUtils.isValid;
import static it.gov.pagopa.wallet.utils.zendesk.FiscalCodeUtils.sanitize;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import org.junit.jupiter.params.provider.Arguments;

class FiscalCodeUtilsTest {

    @ParameterizedTest
    @MethodSource("sanitizeCases")
    void sanitize_worksForAllCases(String input, String expected) {
        assertEquals(expected, sanitize(input));
    }

    static Stream<Arguments> sanitizeCases() {
        return Stream.of(

                arguments(null, null),
                arguments("abC12!@#de f34", "ABC12DEF34"),
                arguments("tint-abcd1234efgh5678", "ABCD1234EFGH5678"),
                arguments("abcDEF123", "ABCDEF123"),
                arguments("TINT-@@@a b c 1 2 3", "ABC123")
        );
    }


    @ParameterizedTest
    @MethodSource("invalidCodes")
    void isValid_returnsFalse_forInvalidInputs(String cf) {
        assertFalse(isValid(cf));
    }

    static Stream<String> invalidCodes() {
        return Stream.of(
                null,
                "",
                "   ",
                "ABC123",
                "ABCDEFGHIJKLMNOPQRST",
                "A".repeat(15),
                "A".repeat(17)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ABCDEF12G34H567I",
            "0123456789ABCDEF"
    })
    void isValid_returnsTrue_forLength16(String cf) {
        assertTrue(isValid(cf));
    }

    @Test
    void constructor_isPrivate_andInvocableViaReflection() throws Exception {
        Constructor<FiscalCodeUtils> ctor = FiscalCodeUtils.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(ctor.getModifiers()), "Constructor must be private");
        ctor.setAccessible(true);
        ctor.newInstance();
    }
}
