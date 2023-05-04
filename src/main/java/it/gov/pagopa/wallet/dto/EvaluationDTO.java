package it.gov.pagopa.wallet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EvaluationDTO {

  @NotEmpty
  private String userId;
  private String familyId;
  @NotEmpty
  private String initiativeId;
  private String initiativeName;
  private LocalDate initiativeEndDate;
  private String organizationId;
  @NotEmpty
  private String status;
  @NotNull
  private LocalDateTime admissibilityCheckDate;
  private LocalDateTime criteriaConsensusTimestamp;
  @NotNull
  private List<OnboardingRejectionReason> onboardingRejectionReasons;
  private BigDecimal beneficiaryBudget;
  private String initiativeRewardType;
  private String organizationName;
  private Boolean isLogoPresent;
}