package it.gov.pagopa.wallet.service.zendesk;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import it.gov.pagopa.wallet.config.zendesk.SupportProperties;
import it.gov.pagopa.wallet.dto.zendesk.SupportRequestDTO;
import it.gov.pagopa.wallet.dto.zendesk.SupportResponseDTO;
import it.gov.pagopa.wallet.utils.zendesk.FiscalCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final String redirectUriBase;
    private final String organization;
    private final String defaultProductId;
    private final Clock clock;

    @Autowired
    public SupportService(SupportProperties properties, @Autowired(required = false) Clock clock) {
        this.jwtKey = Keys.hmacShaKeyFor(properties.getKey().getBytes(StandardCharsets.UTF_8));
        this.redirectUriBase = properties.getZendesk().getRedirectUri();
        this.organization = properties.getZendesk().getOrganization();
        this.defaultProductId = properties.getDefaultProductId();
        this.clock = (clock != null) ? clock : Clock.systemUTC();

        log.info("[ZENDESK-CONNECTOR-SERVICE] initialized, org='{}'", organization);
    }

    public SupportResponseDTO buildJwtAndReturnTo(SupportRequestDTO dto) {

        final String email = dto.email();

        if (StringUtils.isBlank(email)) {
            throw new IllegalArgumentException("Email is required");
        }

        final String name = StringUtils.substringBefore(email, "@");

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

    private String createZendeskJwt(String email, String name, Map<String, Object> userFields) {
        Instant now = Instant.now(clock);
        var builder = Jwts.builder()
                .issuedAt(Date.from(now))
                .id(UUID.randomUUID().toString())
                .claim("email", email)
                .claim("organization", organization)
                .claim("user_fields", userFields)
                .expiration(Date.from(now.plusSeconds(5L * 60L)));

        builder.claim("name", name);

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
