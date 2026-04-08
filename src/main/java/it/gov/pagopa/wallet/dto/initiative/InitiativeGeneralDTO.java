package it.gov.pagopa.wallet.dto.initiative;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import lombok.Data;

@Data
public class InitiativeGeneralDTO   {

  @JsonProperty("budgetCents")
  private Long budgetCents;

  @JsonProperty("beneficiaryType")
  private String beneficiaryType;

  @JsonProperty("beneficiaryKnown")
  private Boolean beneficiaryKnown;

  @JsonProperty("beneficiaryBudgetCents")
  private Long beneficiaryBudgetCents;

  @JsonProperty("startDate")
  private Instant startDate;

  @JsonProperty("endDate")
  private Instant endDate;

  @JsonProperty("rankingStartDate")
  private Instant rankingStartDate;

  @JsonProperty("rankingEndDate")
  private Instant rankingEndDate;

}
