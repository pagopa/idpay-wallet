package it.gov.pagopa.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
@Builder
public class QueueCommandOperationDTO {

    private String operationType;
    private String entityId;
    private Instant operationTime;

}
