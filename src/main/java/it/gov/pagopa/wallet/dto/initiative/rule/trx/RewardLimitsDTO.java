package it.gov.pagopa.wallet.dto.initiative.rule.trx;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class RewardLimitsDTO {

  private String frequency;

  private BigDecimal rewardLimit;
}
