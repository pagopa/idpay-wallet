package it.gov.pagopa.wallet.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.time.LocalDate;

import it.gov.pagopa.wallet.enums.Channel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(Include.NON_NULL)
public class NotificationQueueDTO {
  private String operationType;
  private String userId;
  private String initiativeId;
  private String iban;
  private String status;
  private String rewardNotificationId;
  private String refundCro;
  private LocalDate refundDate;
  private Long refundReward;
  private String rejectionCode;
  private String rejectionReason;
  private Long refundFeedbackProgressive;
  private String initiativeName;
  private String name;
  private String surname;
  private String userMail;
  private Channel channel;
  private String serviceId;
}
