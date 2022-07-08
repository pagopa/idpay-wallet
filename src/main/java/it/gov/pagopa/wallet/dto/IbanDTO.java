package it.gov.pagopa.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor

public class IbanDTO {
    String iban;
    String description;
    String holderBank;
    String channel;
}
