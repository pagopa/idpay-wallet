package it.gov.pagopa.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InstrumentDetailDTO {
    @JsonProperty("maskedPan")
    private String maskedPan;
    @JsonProperty("brand")
    private String brand;
    @JsonProperty("initiativeList")
    private List<StatusOnInitiativeDTO> initiativeList;

}
