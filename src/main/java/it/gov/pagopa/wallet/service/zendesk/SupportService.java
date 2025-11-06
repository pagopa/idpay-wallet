package it.gov.pagopa.wallet.service.zendesk;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import it.gov.pagopa.wallet.dto.zendesk.SupportRequestDTO;
import it.gov.pagopa.wallet.dto.zendesk.SupportResponseDTO;
import it.gov.pagopa.wallet.utils.zendesk.FiscalCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class SupportService {

    private final SecretKey jwtKey;
    private final Clock clock;
    private final String redirectUriBase;
    private final String organization;
    private final String defaultProductId;

    public SupportService(
            @Value("${support.api.key}") String secret,
            @Value("${support.api.zendesk.redirectUri}") String redirectUriBase,
            @Value("${support.api.zendesk.organization}") String organization,
            @Value("${support.api.defaultProductId:}") String defaultProductId,
            @Autowired(required=false)Clock clock
    ) {
        this.jwtKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.redirectUriBase = redirectUriBase;
        this.organization = organization;
        this.defaultProductId = defaultProductId;
        this.clock = (clock != null) ? clock : Clock.systemUTC();
        log.info("[ZENDESK-CONNECTOR-SERVICE] initialized, org='{}'", organization);
    }

    public SupportResponseDTO buildJwtAndReturnTo(SupportRequestDTO dto) {

        final String email = dto.email();

        final String name = fullNameOrNull(dto.firstName(), dto.lastName());

        Map<String, Object> userFields = new HashMap<>();
        String sanitizedCf = FiscalCodeUtils.sanitize(dto.fiscalCode());
        if (FiscalCodeUtils.isValid(sanitizedCf)) {
            userFields.put("aux_data", sanitizedCf);
        }

        String jwt = createZendeskJwt(email, name, userFields);
        String returnTo = buildReturnTo(dto.productId());

        log.info("[ZENDESK-CONNECTOR-SERVICE] built jwt+returnTo");
        return new SupportResponseDTO(jwt, returnTo);
    }

    private static String fullNameOrNull(String first, String last) {
        String fn = first == null ? "" : first.trim();
        String ln = last  == null ? "" : last.trim();
        String full = (fn + " " + ln).trim();
        return full.isEmpty() ? null : full;
    }

    private String createZendeskJwt(String email, String name, Map<String, Object> userFields) {
        Instant now = Instant.now(clock);
        var builder = Jwts.builder()
                .issuedAt(Date.from(now))
                .id(UUID.randomUUID().toString())
                .claim("email", email)
                .claim("organization", organization)
                .claim("user_fields", userFields)
                .expiration(Date.from(now.plusSeconds(5L * 60L)));

        if (StringUtils.isBlank(name)) {
            builder.claim("name", StringUtils.substringBefore(email, "@"));
        } else {
            builder.claim("name", name);
        }

        return builder.signWith(jwtKey).compact();
    }

    private String buildReturnTo(String productId) {
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(redirectUriBase);
        String product = resolveProduct(productId);
        if (isNotBlank(product)) {
            b.queryParam("product", product);
        }
        return b.build(true).toUriString();
    }

    private String resolveProduct(String productId) {
        if (isNotBlank(productId)) return productId;
        return isNotBlank(defaultProductId) ? defaultProductId : null;
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }

}
