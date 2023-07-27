package it.gov.pagopa.wallet.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import it.gov.pagopa.wallet.dto.ErrorDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Utilities {

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.initiative.logo.url}")
    private String logoUrl;

    public static final String LOGO_PATH_TEMPLATE = "assets/logo/%s/%s/%s";
    public static final String LOGO_NAME = "logo.png";

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
