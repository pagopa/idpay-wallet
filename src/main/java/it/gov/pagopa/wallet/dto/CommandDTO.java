package it.gov.pagopa.wallet.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CommandDTO {

    String operationId;
    String operationType;
    LocalDateTime operationTime;

}
