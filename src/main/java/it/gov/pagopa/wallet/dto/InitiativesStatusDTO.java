package it.gov.pagopa.wallet.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Builder
@Getter
@AllArgsConstructor
public class InitiativesStatusDTO {

    String initiativeId;
    String initiativeName;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String idInstrument;
    String status;

}
