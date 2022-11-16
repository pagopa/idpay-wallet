package it.gov.pagopa.wallet.dto;

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
  private LocalDateTime feedbackDate;
  private Long feedbackProgressive;
  private LocalDateTime executionDate;
  private String cro;
}
