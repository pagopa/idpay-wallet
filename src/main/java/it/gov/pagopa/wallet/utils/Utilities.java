package it.gov.pagopa.wallet.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Utilities {
    private final ObjectMapper objectMapper;
    private final String logoUrl;

    public static final String LOGO_PATH_TEMPLATE = "logos/%s/%s/%s";
    public static final String LOGO_NAME = "logo.png";

    public Utilities(ObjectMapper objectMapper,
                     @Value("${app.initiative.logo.url}") String logoUrl) {
        this.objectMapper = objectMapper;
        this.logoUrl = logoUrl;
    }

    public String exceptionConverter(FeignException e) {
        String error;
        try {
            ErrorDTO errorDTO = objectMapper.readValue(e.contentUTF8(), ErrorDTO.class);
            error = errorDTO.getMessage() != null ? errorDTO.getMessage() : e.getMessage();
        } catch (Exception ex) {
            error= e.getMessage();
        }

        return error;
    }

    public String createLogoUrl(String organizationId, String initiativeId){
        return this.logoUrl+this.getPathLogo(organizationId,initiativeId);
    }
    public String getPathLogo(String organizationId, String initiativeId){
        return String.format(LOGO_PATH_TEMPLATE, organizationId, initiativeId, LOGO_NAME);
    }
}
