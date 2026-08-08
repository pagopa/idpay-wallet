package it.gov.pagopa.wallet.dto;

import it.gov.pagopa.wallet.enums.BeneficiaryType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class RefundDTO {
  private String id;
  private String externalId;
  private String rewardNotificationId;
  private String initiativeId;
  private String beneficiaryId;
  private BeneficiaryType beneficiaryType;
  private String organizationId;
  private String iban;
  private String status;
  private String rewardStatus;
  private String refundType;
  private Long rewardCents;
  private Long effectiveRewardCents;
  private Instant startDate;
  private Instant endDate;
  private Instant feedbackDate;
  private String rejectionCode;
  private String rejectionReason;
  private Long feedbackProgressive;
  private Instant executionDate;
  private Instant transferDate;
  private Instant userNotificationDate;
  private String cro;
}
