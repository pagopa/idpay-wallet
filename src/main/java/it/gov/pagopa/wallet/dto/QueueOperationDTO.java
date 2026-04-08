package it.gov.pagopa.wallet.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueueOperationDTO {

  private String userId;

  private String initiativeId;

  private String operationType;

  private String rewardNotificationId;

  private String eventId;

  private String brandLogo;

  private String brand;

  private String maskedPan;

  private String instrumentId;

  private String iban;

  private String channel;

  private String instrumentType;

  private String circuitType;

  private String cro;

  private Instant operationDate;

  private Long rewardFeedbackProgressive;

  private Long amountCents;

  private Long effectiveAmountCents;

  private Long accruedCents;

  private String idTrxIssuer;

  private String idTrxAcquirer;

  private String status;

  private String refundType;

  private Instant startDate;

  private Instant endDate;

  private Instant transferDate;

  private Instant userNotificationDate;

  private String businessName;

}

