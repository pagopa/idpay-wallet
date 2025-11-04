package it.gov.pagopa.wallet.service.zendesk;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.MacAlgorithm;
import it.gov.pagopa.wallet.dto.zendesk.SupportRequestDTO;
import it.gov.pagopa.wallet.utils.zendesk.FiscalCodeUtils;
import it.gov.pagopa.wallet.utils.zendesk.NameAliasUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class SupportService {

    private final Key jwtKey;
    private final Clock clock;
    private final String actionUrl;
    private final String redirectUriBase;
    private final String organization;
    private final String defaultProductId;

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
    }

    public String buildSsoHtml(SupportRequestDTO dto) {
        // 1) Sanifica CF (se lo invii come user_fields)
        String cf = FiscalCodeUtils.sanitize(dto.fiscalCode());
        if (!FiscalCodeUtils.isValid(cf)) {
            throw new IllegalArgumentException("Invalid fiscalCode");
        }

        // 2) JWT Opzione 3: NO external_id. SOLO email (+ name, organization, user_fields)
        Map<String,Object> userFields = new HashMap<>();
        userFields.put("aux_data", cf);

        String name = (dto.name() != null && !dto.name().isBlank())
                ? dto.name().trim() : NameAliasUtils.aliasFromEmail(dto.email());


        Instant now = Instant.now(clock);
        String jwt = Jwts.builder()
                .issuedAt(Date.from(now))
                .id(UUID.randomUUID().toString())
                .claim("name", name)
                .claim("email", dto.email())
                .claim("organization", organization)
                .claim("user_fields", userFields)
                .expiration(Date.from(now.plusSeconds(5L * 60L)))
                .signWith(jwtKey)
                .compact();

        String product = dto.productId();
        if (product == null || product.isBlank()) {
            if (defaultProductId != null && !defaultProductId.isBlank()) {
                product = defaultProductId;
            } else {
                product = null;
            }
        }

        UriComponentsBuilder u = UriComponentsBuilder.fromUriString(redirectUriBase);
        if (product != null && !product.isBlank()) u.queryParam("product", product);
        if (dto.data() != null && !dto.data().isBlank()) u.queryParam("data", dto.data());
        String returnTo = u.build(true).toUriString();

        return """
      <html>
        <head><meta charset="utf-8"/></head>
        <body>
          <form id="jwtForm" method="POST" action="%s">
            <input type="hidden" name="jwt" value="%s"/>
            <input type="hidden" name="return_to" value="%s"/>
          </form>
          <script>window.onload = () => { document.getElementById('jwtForm').submit(); };</script>
        </body>
      </html>
      """.formatted(
                HtmlUtils.htmlEscape(actionUrl),
                HtmlUtils.htmlEscape(jwt),
                HtmlUtils.htmlEscape(returnTo)
        );
    }
}