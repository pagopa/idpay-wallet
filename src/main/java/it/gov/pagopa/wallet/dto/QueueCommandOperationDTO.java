package it.gov.pagopa.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class QueueCommandOperationDTO {

    private String operationType;
    private String entityId;
    private LocalDateTime operationTime;

}
