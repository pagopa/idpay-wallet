package it.gov.pagopa.wallet.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import it.gov.pagopa.wallet.dto.ErrorDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Utilities {

    @Autowired
    private ObjectMapper objectMapper;

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
}
