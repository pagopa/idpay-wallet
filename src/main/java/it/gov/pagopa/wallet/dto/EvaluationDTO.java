package it.gov.pagopa.wallet.dto;

import it.gov.pagopa.wallet.enums.Channel;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
@ToString(callSuper = true)
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
  private Long beneficiaryBudgetCents;
  private String initiativeRewardType;
  private String organizationName;
  private Boolean isLogoPresent;
  private Long maxTrx;
  private String serviceId;
  private Channel channel;
  private String userMail;
  private String name;
  private String surname;
}