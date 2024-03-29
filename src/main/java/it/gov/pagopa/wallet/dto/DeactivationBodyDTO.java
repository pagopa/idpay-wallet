package it.gov.pagopa.wallet.dto;

import it.gov.pagopa.wallet.constants.WalletConstants;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeactivationBodyDTO {

  @NotBlank(message = WalletConstants.ERROR_MANDATORY_FIELD)
  String userId;

  @NotBlank(message = WalletConstants.ERROR_MANDATORY_FIELD)
  String initiativeId;

  @NotBlank(message = WalletConstants.ERROR_MANDATORY_FIELD)
  String instrumentId;

  @NotBlank(message = WalletConstants.ERROR_MANDATORY_FIELD)
  String channel;
}

