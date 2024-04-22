package it.gov.pagopa.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RewardDTO {

  private Long providedRewardCents;
  private Long accruedRewardCents;
  private boolean capped;
  private boolean dailyCapped;
  private boolean monthlyCapped;
  private boolean yearlyCapped;
  private boolean weeklyCapped;
  private Counters counters;
}
