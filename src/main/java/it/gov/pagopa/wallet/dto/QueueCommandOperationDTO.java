package it.gov.pagopa.wallet.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class QueueCommandOperationDTO {

    String operationType;
    String entityId;
    LocalDateTime operationTime;

}
