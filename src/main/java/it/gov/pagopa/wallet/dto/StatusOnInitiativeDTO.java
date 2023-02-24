package it.gov.pagopa.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StatusOnInitiativeDTO {
    @JsonProperty("initiativeId")
    String initiativeId;
    @JsonProperty("idInstrument")
    String idInstrument;
    @JsonProperty("status")
    String status;
}
