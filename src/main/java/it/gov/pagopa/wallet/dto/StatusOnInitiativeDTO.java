package it.gov.pagopa.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StatusOnInitiativeDTO {
    String initiativeId;
    String status;
}
