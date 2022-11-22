package it.gov.pagopa.wallet.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RefundDTO {
  private String rewardNotificationId;
  private String initiativeId;
  private String userId;
  private String organizationId;
  private String status;
  private Long rewardCents;
  private Long effectiveRewardCents;
  private LocalDateTime feedbackDate;
  private String rejectionCode;
  private String rejectionReason;
  private Long feedbackProgressive;
  private LocalDate executionDate;
  private String cro;
}
