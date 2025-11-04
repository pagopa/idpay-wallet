package it.gov.pagopa.wallet.service.zendesk;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import it.gov.pagopa.wallet.dto.zendesk.SupportRequestDTO;
import it.gov.pagopa.wallet.utils.zendesk.FiscalCodeUtils;
import it.gov.pagopa.wallet.utils.zendesk.NameAliasUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.crypto.SecretKey;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class SupportServiceTest {

    private static final String SECRET_32 = "0123456789ABCDEF0123456789ABCDEF";
    private static final String ACTION_URL_WITH_QUERY = "https://pagopa.zendesk.com/access/jwt?x=1&y=2";
    private static final String REDIRECT_BASE = "https://bonus.assistenza.pagopa.it/requests/new";
    private static final String ORG = "_users_hc_bonus";
    private static final String DEFAULT_PRODUCT = "DEF-PROD";
    private static final Instant FIXED_NOW = Instant.parse("2025-01-01T10:00:00Z");

    private Clock fixedClock;
    private SupportService service;

    @BeforeEach
    void setup() {
        fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        service = new SupportService(
                SECRET_32,
                ACTION_URL_WITH_QUERY,
                REDIRECT_BASE,
                ORG,
                DEFAULT_PRODUCT,
                fixedClock
        );
    }

    private SupportRequestDTO mockDto(
            String email,
            String name,
            String cf,
            String ticketFormId,
            String subject,
            String message,
            String productId,
            String data,
            Map<String,String> customFields
    ) {
        SupportRequestDTO dto = mock(SupportRequestDTO.class);
        when(dto.email()).thenReturn(email);
        when(dto.name()).thenReturn(name);
        when(dto.fiscalCode()).thenReturn(cf);
        when(dto.ticketFormId()).thenReturn(ticketFormId);
        when(dto.subject()).thenReturn(subject);
        when(dto.message()).thenReturn(message);
        when(dto.productId()).thenReturn(productId);
        when(dto.data()).thenReturn(data);
        when(dto.customFields()).thenReturn(customFields);
        return dto;
    }

    @Test
    void buildSsoHtml_success_allFields_present_andHtmlEscaped_andJwtClaimsOk() {
        try (MockedStatic<FiscalCodeUtils> cfMock = mockStatic(FiscalCodeUtils.class)) {
            cfMock.when(() -> FiscalCodeUtils.sanitize("ABCDEF12G34H567I")).thenReturn("ABCDEF12G34H567I");
            cfMock.when(() -> FiscalCodeUtils.isValid("ABCDEF12G34H567I")).thenReturn(true);

            Map<String,String> custom = new HashMap<>();
            custom.put("1001", "TAG_A");
            custom.put("", "SKIP");
            custom.put("2002", "");

            String longSubject = "S".repeat(200);
            String longDesc = "D".repeat(6000);

            SupportRequestDTO dto = mockDto(
                    "user@example.org",
                    "Mario Rossi",
                    "ABCDEF12G34H567I",
                    "999999",
                    longSubject,
                    longDesc,
                    "PRD-123",
                    "meta",
                    custom
            );

            String html = service.buildSsoHtml(dto);
            assertNotNull(html);
            assertTrue(html.contains("<form id=\"jwtForm\" method=\"POST\""), "deve generare form auto-post");
            assertTrue(html.contains("action=\"https://pagopa.zendesk.com/access/jwt?x=1&amp;y=2\""),
                    "action URL deve essere escaped");

            String jwt = extractHiddenInput(html, "jwt");
            String returnToEscaped = extractHiddenInput(html, "return_to");
            assertNotNull(jwt);
            assertNotNull(returnToEscaped);

            String returnTo = htmlUnescape(returnToEscaped);

            assertTrue(returnTo.startsWith(REDIRECT_BASE), "return_to deve iniziare dalla base");
            assertTrue(returnTo.contains("ticket_form_id=999999"));
            assertTrue(returnTo.contains("product=PRD-123"));
            assertTrue(returnTo.contains("data=meta"));
            assertTrue(returnTo.contains("tf_1001=TAG_A"), "custom field valido deve esserci");
            assertFalse(returnTo.contains("tf_2002="), "custom field invalido non deve esserci");

            String expectedSubject = "S".repeat(150);
            String expectedDesc = "D".repeat(5000);
            String decodedReturnTo = URLDecoder.decode(returnTo, StandardCharsets.UTF_8);
            assertTrue(decodedReturnTo.contains("subject=" + expectedSubject));
            assertTrue(decodedReturnTo.contains("description=" + expectedDesc));

            SecretKey key = Keys.hmacShaKeyFor(SECRET_32.getBytes(StandardCharsets.UTF_8));
            io.jsonwebtoken.Clock jjwtClock = () -> Date.from(FIXED_NOW);

            Jws<Claims> parsed = Jwts.parser()
                    .verifyWith(key)
                    .clock(jjwtClock)
                    .build()
                    .parseSignedClaims(jwt);

            Claims claims = parsed.getPayload();

            assertEquals("Mario Rossi", claims.get("name"));
            assertEquals("user@example.org", claims.get("email"));
            assertEquals(ORG, claims.get("organization"));

            @SuppressWarnings("unchecked")
            Map<String, Object> userFields = (Map<String, Object>) claims.get("user_fields");
            assertEquals("ABCDEF12G34H567I", userFields.get("aux_data"));

            long expSeconds = claims.getExpiration().toInstant().getEpochSecond();
            assertEquals(FIXED_NOW.plusSeconds(300).getEpochSecond(), expSeconds);
        }
    }

    @Test
    void buildSsoHtml_blankName_usesAliasFromEmail() {
        try (MockedStatic<FiscalCodeUtils> cfMock = mockStatic(FiscalCodeUtils.class);
             MockedStatic<NameAliasUtils> aliasMock = mockStatic(NameAliasUtils.class)) {

            cfMock.when(() -> FiscalCodeUtils.sanitize("ABCDEF12G34H567I")).thenReturn("ABCDEF12G34H567I");
            cfMock.when(() -> FiscalCodeUtils.isValid("ABCDEF12G34H567I")).thenReturn(true);
            aliasMock.when(() -> NameAliasUtils.aliasFromEmail("alias@dom.it")).thenReturn("aliasDom");

            SupportRequestDTO dto = mockDto(
                    "alias@dom.it",
                    "  ",
                    "ABCDEF12G34H567I",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            String html = service.buildSsoHtml(dto);
            String jwt = extractHiddenInput(html, "jwt");

            SecretKey key = Keys.hmacShaKeyFor(SECRET_32.getBytes(StandardCharsets.UTF_8));
            io.jsonwebtoken.Clock jjwtClock = () -> Date.from(FIXED_NOW);

            Jws<Claims> parsed = Jwts.parser()
                    .verifyWith(key)
                    .clock(jjwtClock)
                    .build()
                    .parseSignedClaims(jwt);

            assertEquals("aliasDom", parsed.getPayload().get("name"));
        }
    }

    @Test
    void buildSsoHtml_invalidFiscalCode_throwsIllegalArgument() {
        try (MockedStatic<FiscalCodeUtils> cfMock = mockStatic(FiscalCodeUtils.class)) {
            cfMock.when(() -> FiscalCodeUtils.sanitize("BAD_CF")).thenReturn("BAD_CF");
            cfMock.when(() -> FiscalCodeUtils.isValid("BAD_CF")).thenReturn(false);

            SupportRequestDTO dto = mockDto(
                    "user@example.org",
                    "Mario",
                    "BAD_CF",
                    null, null, null, null, null, null
            );
            assertThrows(IllegalArgumentException.class, () -> service.buildSsoHtml(dto));
        }
    }

    @Test
    void buildSsoHtml_usesDefaultProduct_whenNotProvided() {
        try (MockedStatic<FiscalCodeUtils> cfMock = mockStatic(FiscalCodeUtils.class)) {
            cfMock.when(() -> FiscalCodeUtils.sanitize("ABCDEF12G34H567I")).thenReturn("ABCDEF12G34H567I");
            cfMock.when(() -> FiscalCodeUtils.isValid("ABCDEF12G34H567I")).thenReturn(true);

            SupportRequestDTO dto = mockDto(
                    "user@example.org",
                    "Mario",
                    "ABCDEF12G34H567I",
                    "888",
                    "subj",
                    "body",
                    "  ",
                    null,
                    Map.of()
            );

            String html = service.buildSsoHtml(dto);
            String returnTo = htmlUnescape(extractHiddenInput(html, "return_to"));
            assertTrue(returnTo.contains("product=" + DEFAULT_PRODUCT));
        }
    }

    @Test
    void htmlContainsCorrectEnctypeAndAutopostScript() {
        try (MockedStatic<FiscalCodeUtils> cfMock = mockStatic(FiscalCodeUtils.class)) {
            cfMock.when(() -> FiscalCodeUtils.sanitize("ABCDEF12G34H567I")).thenReturn("ABCDEF12G34H567I");
            cfMock.when(() -> FiscalCodeUtils.isValid("ABCDEF12G34H567I")).thenReturn(true);

            SupportRequestDTO dto = mockDto(
                    "user@example.org",
                    "Mario",
                    "ABCDEF12G34H567I",
                    null, null, null, null, null, null
            );
            String html = service.buildSsoHtml(dto);
            assertTrue(html.contains("enctype=\"application/x-www-form-urlencoded\""));
            assertTrue(html.contains("document.getElementById('jwtForm').submit();"));
        }
    }

    private String extractHiddenInput(String html, String name) {
        String token = "name=\"" + name + "\" value=\"";
        int start = html.indexOf(token);
        assertTrue(start >= 0, "input hidden " + name + " non trovato");
        int from = start + token.length();
        int end = html.indexOf("\"", from);
        assertTrue(end > from, "value non trovato per " + name);
        return html.substring(from, end);
    }

    private String htmlUnescape(String s) {
        return s.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }
}
