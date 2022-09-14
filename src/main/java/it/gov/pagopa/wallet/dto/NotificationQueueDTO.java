package it.gov.pagopa.wallet.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationQueueDTO {
  private String operationType;
  private String userId;
  private String initiativeId;
  private String serviceId;
  private String iban;
  private String status;

}
