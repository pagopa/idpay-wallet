package it.gov.pagopa.wallet.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueueOperationDTO {

  private String userId;

  private String initiativeId;

  private String operationType;

  private LocalDateTime operationDate;

}

