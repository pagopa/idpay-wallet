package it.gov.pagopa.wallet.service.zendesk;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import it.gov.pagopa.wallet.dto.zendesk.SupportRequestDTO;
import it.gov.pagopa.wallet.utils.zendesk.FiscalCodeUtils;
import it.gov.pagopa.wallet.utils.zendesk.NameAliasUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class SupportService {

    private final SecretKey jwtKey;
    private final Clock clock;
    private final String actionUrl;       // https://pagopa.zendesk.com/access/jwt
    private final String redirectUriBase; // https://bonus.assistenza.pagopa.it/requests/new
    private final String organization;    // _users_hc_bonus
    private final String defaultProductId;

    private static final int SUBJECT_MAX = 150;
    private static final int DESC_MAX = 5000;

    public SupportService(@Value("${support.jwtSecret}") String secret,
                          @Value("${support.zendesk.actionUri}") String actionUrl,
                          @Value("${support.zendesk.redirectUri}") String redirectUriBase,
                          @Value("${support.zendesk.organization}") String organization,
                          @Value("${support.defaultProductId:}") String defaultProductId,
                          @Autowired(required = false) Clock clock) {
        this.jwtKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.actionUrl = actionUrl;
        this.redirectUriBase = redirectUriBase;
        this.organization = organization;
        this.defaultProductId = defaultProductId;
        this.clock = clock;
        log.info("[ZENDESK-CONNECTOR-SERVICE] - SupportService initialized with organization '{}'", organization);
    }

    public String buildSsoHtml(SupportRequestDTO dto) {
        log.info("[ZENDESK-CONNECTOR-SERVICE] - Building SSO HTML for email '{}'", dto.email());

        final String cf = sanitizeAndValidateCf(dto.fiscalCode());
        final String name = resolveName(dto.name(), dto.email());
        final Map<String, Object> userFields = buildUserFields(cf);
        final String jwt = createJwt(dto.email(), name, userFields);
        final String returnTo = buildReturnTo(dto);

        log.info("[ZENDESK-CONNECTOR-SERVICE] - Successfully built JWT and returnTo URL for user '{}'", dto.email());
        return renderAutoPostHtml(jwt, returnTo);
    }

    private String sanitizeAndValidateCf(String rawCf) {
        log.info("[ZENDESK-CONNECTOR-SERVICE] - Sanitizing and validating fiscal code");
        final String cf = FiscalCodeUtils.sanitize(rawCf);
        if (!FiscalCodeUtils.isValid(cf)) {
            log.info("[ZENDESK-CONNECTOR-SERVICE] - Invalid fiscal code '{}'", rawCf);
            throw new IllegalArgumentException("Invalid fiscalCode");
        }
        return cf;
    }

    private String resolveName(String name, String email) {
        if (isBlank(name)) {
            log.info("[ZENDESK-CONNECTOR-SERVICE] - Name not provided, resolving alias from email '{}'", email);
            return NameAliasUtils.aliasFromEmail(email);
        }
        return name.trim();
    }

    private Map<String, Object> buildUserFields(String cf) {
        log.info("[ZENDESK-CONNECTOR-SERVICE] - Building user fields with fiscal code '{}'", cf);
        return Map.of("aux_data", cf);
    }

    private String createJwt(String email, String name, Map<String, Object> userFields) {
        log.info("[ZENDESK-CONNECTOR-SERVICE] - Creating JWT for user '{}'", email);
        final Instant now = Instant.now(clock);
        final String jwt = Jwts.builder()
                .issuedAt(Date.from(now))
                .id(UUID.randomUUID().toString())
                .claim("name", name)
                .claim("email", email)
                .claim("organization", organization)
                .claim("user_fields", userFields)
                .expiration(Date.from(now.plusSeconds(5L * 60L)))
                .signWith(jwtKey)
                .compact();
        log.info("[ZENDESK-CONNECTOR-SERVICE] - JWT successfully created for '{}'", email);
        return jwt;
    }

    private String buildReturnTo(SupportRequestDTO dto) {
        log.info("[ZENDESK-CONNECTOR-SERVICE] - Building returnTo URL for '{}'", dto.email());

        final UriComponentsBuilder base = UriComponentsBuilder.fromUriString(redirectUriBase);

        addIfPresent(base, "ticket_form_id", dto.ticketFormId());
        addIfPresent(base, "subject", trimOrNull(dto.subject(), SUBJECT_MAX));
        addIfPresent(base, "description", trimOrNull(dto.message(), DESC_MAX));
        addIfPresent(base, "product", resolveProduct(dto.productId()));
        addIfPresent(base, "data", dto.data());

        String tmp = base.build(true).toUriString();
        UriComponentsBuilder withCustom = UriComponentsBuilder.fromUriString(tmp);
        addCustomFields(withCustom, dto.customFields());

        final String result = withCustom.build(true).toUriString();
        log.info("[ZENDESK-CONNECTOR-SERVICE] - returnTo URL built successfully for '{}'", dto.email());
        return result;
    }

    private String resolveProduct(String productId) {
        log.info("[ZENDESK-CONNECTOR-SERVICE] - Resolving productId: {}", productId);
        if (isNotBlank(productId)) return productId;
        return isNotBlank(defaultProductId) ? defaultProductId : null;
    }

    private void addCustomFields(UriComponentsBuilder b, Map<String, String> customFields) {
        if (customFields == null || customFields.isEmpty()) return;
        log.info("[ZENDESK-CONNECTOR-SERVICE] - Adding {} custom fields", customFields.size());
        customFields.forEach((fieldId, valueTag) -> {
            if (isNotBlank(fieldId) && isNotBlank(valueTag)) {
                b.queryParam("tf_" + fieldId, valueTag);
            }
        });
    }

    private void addIfPresent(UriComponentsBuilder b, String key, String value) {
        if (isNotBlank(value)) {
            log.info("[ZENDESK-CONNECTOR-SERVICE] - Adding query param '{}' = '{}'", key, value);
            b.queryParam(key, value);
        }
    }

    private String renderAutoPostHtml(String jwt, String returnTo) {
        log.info("[ZENDESK-CONNECTOR-SERVICE] - Rendering auto-post HTML");
        return """
      <html>
        <head><meta charset="utf-8"/></head>
        <body>
          <form id="jwtForm" method="POST" action="%s" enctype="%s">
            <input type="hidden" name="jwt" value="%s"/>
            <input type="hidden" name="return_to" value="%s"/>
          </form>
          <script>window.onload = () => { document.getElementById('jwtForm').submit(); };</script>
        </body>
      </html>
      """.formatted(
                HtmlUtils.htmlEscape(actionUrl),
                HtmlUtils.htmlEscape(MediaType.APPLICATION_FORM_URLENCODED_VALUE),
                HtmlUtils.htmlEscape(jwt),
                HtmlUtils.htmlEscape(returnTo)
        );
    }

    private static String trimOrNull(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        return t.length() > max ? t.substring(0, max) : t;
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static boolean isNotBlank(String s) { return !isBlank(s); }
}
