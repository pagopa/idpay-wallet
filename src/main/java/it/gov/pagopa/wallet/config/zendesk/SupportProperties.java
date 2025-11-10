package it.gov.pagopa.wallet.config.zendesk;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "support.api")
@Getter
@Setter
public class SupportProperties {

    private String key;
    private Zendesk zendesk = new Zendesk();
    private String defaultProductId;

    @Getter
    @Setter
    public static class Zendesk {
        private String actionUri;
        private String redirectUri;
        private String organization;

    }
}