package it.gov.pagopa.wallet.dto.initiative;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
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
  private LocalDate startDate;

  @JsonProperty("endDate")
  private LocalDate endDate;

  @JsonProperty("rankingStartDate")
  private LocalDate rankingStartDate;

  @JsonProperty("rankingEndDate")
  private LocalDate rankingEndDate;

}
