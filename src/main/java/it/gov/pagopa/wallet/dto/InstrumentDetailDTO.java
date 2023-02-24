package it.gov.pagopa.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class InstrumentDetailDTO {
    @JsonProperty("maskedPan")
    String maskedPan;
    @JsonProperty("brand")
    String brand;
    @JsonProperty("initiativeList")
    List<StatusOnInitiativeDTO> initiativeList;

}
