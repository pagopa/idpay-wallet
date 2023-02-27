package it.gov.pagopa.wallet.dto;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
public class InstrumentStatusOnInitiativeDTO {

    String initiativeId;
    String initiativeName;
    String idInstrument;
    String status;

}
