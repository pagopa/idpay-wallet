package it.gov.pagopa.wallet.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.gov.pagopa.wallet.utils.json.BigDecimalScale2Deserializer;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Counters {
  private boolean exhaustedBudget;
  private Long trxNumber;
  @JsonDeserialize(using = BigDecimalScale2Deserializer.class)
  private BigDecimal totalReward;
  @JsonDeserialize(using = BigDecimalScale2Deserializer.class)
  private BigDecimal initiativeBudget;
  @JsonDeserialize(using = BigDecimalScale2Deserializer.class)
  private BigDecimal totalAmount;
  private Long version;
}
