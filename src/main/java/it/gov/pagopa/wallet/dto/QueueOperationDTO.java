package it.gov.pagopa.wallet.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueueOperationDTO {

  private String userId;

  private String initiativeId;

  private String operationType;

  private String brandLogo;

  private String maskedPan;

  private String instrumentId;

  private String hpan;

  private String iban;

  private String email;

  private String channel;

  private String circuitType;

  private LocalDateTime operationDate;

  private BigDecimal amount;

  private BigDecimal effectiveAmount;

  private BigDecimal accrued;

  private String idTrxIssuer;

  private String idTrxAcquirer;

  private String application;
}

