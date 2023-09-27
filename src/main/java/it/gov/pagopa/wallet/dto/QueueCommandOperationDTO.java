package it.gov.pagopa.wallet.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class QueueCommandOperationDTO {

    private String operationType;
    private String entityId;
    private LocalDateTime operationTime;
    private Map<String, String> additionalParams;

}
