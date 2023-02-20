package it.gov.pagopa.wallet.dto;

import it.gov.pagopa.wallet.constants.WalletConstants;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InstrumentIssuerDTO {

  @NotBlank(message = WalletConstants.ERROR_MANDATORY_FIELD)
  String hpan;

  @NotBlank(message = WalletConstants.ERROR_MANDATORY_FIELD)
  String channel;

  String brandLogo;

  String brand;

  String maskedPan;
}
