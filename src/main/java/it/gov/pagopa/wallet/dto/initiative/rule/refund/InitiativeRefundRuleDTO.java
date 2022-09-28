package it.gov.pagopa.wallet.dto.initiative.rule.refund;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class InitiativeRefundRuleDTO {
  @JsonProperty("accumulatedAmount")
  private AccumulatedAmountDTO accumulatedAmount;

  @JsonProperty("timeParameter")
  private TimeParameterDTO timeParameter;

  @JsonProperty("additionalInfo")
  private RefundAdditionalInfoDTO additionalInfo;
}
