package it.gov.pagopa.wallet.dto.initiative.rule.refund;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TimeParameterDTO {

  @JsonProperty("timeType")
  private String timeType;
}
