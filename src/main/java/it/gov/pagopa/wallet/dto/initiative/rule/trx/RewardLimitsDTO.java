package it.gov.pagopa.wallet.dto.initiative.rule.trx;

import lombok.Data;

@Data
public class RewardLimitsDTO {

  private String frequency;

  private Long rewardLimitCents;
}
