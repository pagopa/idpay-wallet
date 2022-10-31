package it.gov.pagopa.wallet.dto.initiative.rule.trx;

import java.util.Set;
import lombok.Data;

@Data
public class MccFilterDTO {

  private boolean allowedList;
  private Set<String> values;
}
