package it.gov.pagopa.wallet.dto;

import it.gov.pagopa.wallet.constants.WalletConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.validation.constraints.NotBlank;

@Getter
@AllArgsConstructor
public class IbanBodyDTO {

  @NotBlank(message = WalletConstants.ERROR_MANDATORY_FIELD)
  String initiativeId;

  @NotBlank(message = WalletConstants.ERROR_MANDATORY_FIELD)
  String iban;

  @NotBlank(message = WalletConstants.ERROR_MANDATORY_FIELD)
  String description;

}

