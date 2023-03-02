package it.gov.pagopa.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatusOnInitiativeDTO {
    @JsonProperty("initiativeId")
    private String initiativeId;
    @JsonProperty("idInstrument")
    private String idInstrument;
    @JsonProperty("status")
    private String status;
}
