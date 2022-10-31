package it.gov.pagopa.wallet.dto.initiative.rule.trx;

import lombok.Data;

@Data
public class TrxCountDTO {

  private Long from;

  private Boolean fromIncluded;

  private Long to;

  private Boolean toIncluded;
}
