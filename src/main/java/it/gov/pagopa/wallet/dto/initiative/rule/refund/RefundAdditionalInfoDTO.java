package it.gov.pagopa.wallet.dto.initiative.rule.refund;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RefundAdditionalInfoDTO {
  @JsonProperty("identificationCode")
  private String identificationCode;
}
