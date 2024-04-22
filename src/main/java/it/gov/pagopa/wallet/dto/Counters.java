package it.gov.pagopa.wallet.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.gov.pagopa.wallet.utils.json.BigDecimalScale2Deserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Counters {
  private boolean exhaustedBudget;
  private Long trxNumber;
  @JsonDeserialize(using = BigDecimalScale2Deserializer.class)
  private Long totalRewardCents;
  @JsonDeserialize(using = BigDecimalScale2Deserializer.class)
  private Long initiativeBudgetCents;
  @JsonDeserialize(using = BigDecimalScale2Deserializer.class)
  private Long totalAmountCents;
  private Long version;
}
