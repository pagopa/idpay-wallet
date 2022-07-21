package it.gov.pagopa.wallet.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@JsonInclude(Include.NON_NULL)
public class QueueOperationDTO {

  private String userId;

  private String initiativeId;

  private String operationType;

  private String hpan;

  private String iban;

  private String channel;

  private LocalDateTime operationDate;
}

