package it.gov.pagopa.wallet.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RewardDTO {

  private BigDecimal providedReward;
  private BigDecimal accruedReward;
  private boolean capped;
  private boolean dailyCapped;
  private boolean monthlyCapped;
  private boolean yearlyCapped;
  private boolean weeklyCapped;
  private Counters counters;
}
