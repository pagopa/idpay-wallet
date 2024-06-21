package it.gov.pagopa.wallet.dto.initiative.rule.trx;

import lombok.Data;

@Data
public class ThresholdDTO {
  private Long fromCents;

  private Boolean fromIncluded;

  private Long toCents;

  private Boolean toIncluded;
}
