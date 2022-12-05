package it.gov.pagopa.wallet.dto;

import it.gov.pagopa.wallet.constants.WalletConstants;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InstrumentIssuerDTO {

  @NotBlank(message = WalletConstants.ERROR_MANDATORY_FIELD)
  String pgpPan;

  @NotBlank(message = WalletConstants.ERROR_MANDATORY_FIELD)
  String channel;

  int expireYear;

  @NotBlank(message = WalletConstants.ERROR_MANDATORY_FIELD)
  String expireMonth;

  @NotBlank(message = WalletConstants.ERROR_MANDATORY_FIELD)
  String type;

  @NotBlank(message = WalletConstants.ERROR_MANDATORY_FIELD)
  String issuerAbiCode;

  @NotBlank(message = WalletConstants.ERROR_MANDATORY_FIELD)
  String brand;

  String holder;
}
