package it.gov.pagopa.wallet.service.zendesk;

import it.gov.pagopa.wallet.config.zendesk.SupportProperties;
import it.gov.pagopa.wallet.dto.zendesk.SupportRequestDTO;
import it.gov.pagopa.wallet.dto.zendesk.SupportResponseDTO;
import it.gov.pagopa.wallet.utils.zendesk.FiscalCodeUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportServiceTest {

    private static final String SECRET = "this-is-a-test-secret-with->=-32-bytes-length!!!";
    private static final String REDIRECT_BASE = "https://bonus.assistenza.pagopa.it/requests/new";
    private static final String ORG = "_users_hc_bonus";
    private static final String ACTION_URI = "https://pagopa.zendesk.com/access/jwt";

    private static Claims parse(String jwt) {
        var key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }

    private static SupportProperties makeProps(String defaultProductId) {
        var props = new SupportProperties();
        props.setKey(SECRET);

        var z = new SupportProperties.Zendesk();
        z.setActionUri(ACTION_URI);
        z.setRedirectUri(REDIRECT_BASE);
        z.setOrganization(ORG);
        props.setZendesk(z);

        props.setDefaultProductId(defaultProductId);
        return props;
    }

    @Test
    void buildJwtAndReturnTo_validCf_andExplicitProduct_setsEmailLocalPartAsName_andAuxData_andProductQuery() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        var clock = Clock.fixed(now, ZoneOffset.UTC);

        var properties = makeProps("DEF_PROD");
        var service = new SupportService(properties, clock);

        var dto = mock(SupportRequestDTO.class);
        when(dto.email()).thenReturn("user@example.com");
        when(dto.fiscalCode()).thenReturn("ABCDEF12G34H567I");
        when(dto.productId()).thenReturn("PROD123");

        try (MockedStatic<FiscalCodeUtils> mocked = mockStatic(FiscalCodeUtils.class)) {
            mocked.when(() -> FiscalCodeUtils.sanitize("ABCDEF12G34H567I")).thenReturn("SANITIZED_CF");
            mocked.when(() -> FiscalCodeUtils.isValid("SANITIZED_CF")).thenReturn(true);

            SupportResponseDTO resp = service.buildJwtAndReturnTo(dto);

            assertEquals(REDIRECT_BASE + "?product=PROD123", resp.returnTo());

            Claims claims = parse(resp.jwt());
            assertEquals("user@example.com", claims.get("email"));
            assertEquals(ORG, claims.get("organization"));
            assertNotNull(claims.getId());
            assertEquals(now, claims.getIssuedAt().toInstant());
            assertEquals(now.plusSeconds(300), claims.getExpiration().toInstant());
            assertEquals("user", claims.get("name"));

            @SuppressWarnings("unchecked")
            Map<String, Object> userFields = (Map<String, Object>) claims.get("user_fields");
            assertNotNull(userFields);
            assertEquals("SANITIZED_CF", userFields.get("aux_data"));
        }
    }

    @Test
    void buildJwtAndReturnTo_invalidCf_andNoName_usesDefaultProduct_setsEmailLocalPartAsName_andNoAuxData() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        var clock = Clock.fixed(now, ZoneOffset.UTC);

        var properties = makeProps("DEFAULT_PROD");
        var service = new SupportService(properties, clock);

        var dto = mock(SupportRequestDTO.class);
        when(dto.email()).thenReturn("u@e.com");
        when(dto.fiscalCode()).thenReturn("whatever");
        when(dto.productId()).thenReturn(null);

        try (MockedStatic<FiscalCodeUtils> mocked = mockStatic(FiscalCodeUtils.class)) {
            mocked.when(() -> FiscalCodeUtils.sanitize("whatever")).thenReturn("x");
            mocked.when(() -> FiscalCodeUtils.isValid("x")).thenReturn(false);

            SupportResponseDTO resp = service.buildJwtAndReturnTo(dto);

            assertEquals(REDIRECT_BASE + "?product=DEFAULT_PROD", resp.returnTo());

            Claims claims = parse(resp.jwt());
            assertEquals("u@e.com", claims.get("email"));
            assertEquals("u", claims.get("name"));

            @SuppressWarnings("unchecked")
            Map<String, Object> userFields = (Map<String, Object>) claims.get("user_fields");
            assertNotNull(userFields);
            assertFalse(userFields.containsKey("aux_data"));
        }
    }

    @Test
    void buildJwtAndReturnTo_noProductAnywhere_hasNoProductQueryParam_andNameFromEmailLocalPart() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        var clock = Clock.fixed(now, ZoneOffset.UTC);

        var properties = makeProps("");
        var service = new SupportService(properties, clock);

        var dto = mock(SupportRequestDTO.class);
        when(dto.email()).thenReturn("x@y.zz");
        when(dto.fiscalCode()).thenReturn("CF");
        when(dto.productId()).thenReturn("   ");

        try (MockedStatic<FiscalCodeUtils> mocked = mockStatic(FiscalCodeUtils.class)) {
            mocked.when(() -> FiscalCodeUtils.sanitize("CF")).thenReturn("CF");
            mocked.when(() -> FiscalCodeUtils.isValid("CF")).thenReturn(true);

            SupportResponseDTO resp = service.buildJwtAndReturnTo(dto);

            assertEquals(REDIRECT_BASE, resp.returnTo());

            Claims claims = parse(resp.jwt());
            assertEquals("x", claims.get("name"));
        }
    }

    @Test
    void constructor_withNullClock_usesSystemUtc_andStillBuildsJwt_andSetsNameFromEmailLocalPart() {
        var properties = makeProps("");
        var service = new SupportService(properties, null);

        var dto = mock(SupportRequestDTO.class);
        when(dto.email()).thenReturn("a@b.co");
        when(dto.fiscalCode()).thenReturn("CF");
        when(dto.productId()).thenReturn(null);

        try (MockedStatic<FiscalCodeUtils> mocked = mockStatic(FiscalCodeUtils.class)) {
            mocked.when(() -> FiscalCodeUtils.sanitize("CF")).thenReturn("CF");
            mocked.when(() -> FiscalCodeUtils.isValid("CF")).thenReturn(false);

            SupportResponseDTO resp = service.buildJwtAndReturnTo(dto);

            assertNotNull(resp.jwt());
            assertTrue(resp.jwt().length() > 20);
            assertEquals(REDIRECT_BASE, resp.returnTo());
            Claims claims = parse(resp.jwt());
            assertEquals(ORG, claims.get("organization"));
            assertEquals("a", claims.get("name"));
        }
    }

    @Test
    void buildJwtAndReturnTo_blankEmail_throwsIllegalArgumentException() {
        var properties = makeProps("ANY");
        var service = new SupportService(properties, Clock.systemUTC());

        var dto = mock(SupportRequestDTO.class);
        when(dto.email()).thenReturn("   ");

        assertThrows(IllegalArgumentException.class, () -> service.buildJwtAndReturnTo(dto));
    }
}
