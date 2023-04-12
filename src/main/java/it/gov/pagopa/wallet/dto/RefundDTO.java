package it.gov.pagopa.wallet.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RefundDTO {
  private String id;
  private String externalId;
  private String rewardNotificationId;
  private String initiativeId;
  private String userId;
  private String organizationId;
  private String iban;
  private String status;
  private String rewardStatus;
  private String refundType;
  private Long rewardCents;
  private Long effectiveRewardCents;
  private LocalDate startDate;
  private LocalDate endDate;
  private LocalDateTime feedbackDate;
  private String rejectionCode;
  private String rejectionReason;
  private Long feedbackProgressive;
  private LocalDate executionDate;
  private LocalDate transferDate;
  private LocalDate userNotificationDate;
  private String cro;
}
