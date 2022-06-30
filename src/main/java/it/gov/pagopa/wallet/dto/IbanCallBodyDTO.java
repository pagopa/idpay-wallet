package it.gov.pagopa.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor

public class IbanCallBodyDTO {
    String userId;
    String initiativeId;
    String iban;
    String description;
}
