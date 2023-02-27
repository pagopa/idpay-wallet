package it.gov.pagopa.wallet.dto;

import lombok.*;

@Builder
@Getter
@AllArgsConstructor
public class InitiativesStatusDTO {

    String initiativeId;
    String initiativeName;
    String idInstrument;
    String status;

}
