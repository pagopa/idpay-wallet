package it.gov.pagopa.wallet.dto.initiative;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SelfCriteriaMultiConsentValueDTO {

  private String description;

  private String subDescription;

  private String value;

  private boolean verify;

  private String thresholdCode;

  private Long beneficiaryBudgetCentsMin;

  private Long beneficiaryBudgetCentsMax;

  private boolean blockingVerify;

}
