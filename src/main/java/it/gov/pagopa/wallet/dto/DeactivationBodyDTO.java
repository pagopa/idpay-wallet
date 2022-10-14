package it.gov.pagopa.wallet.dto;

import it.gov.pagopa.wallet.constants.WalletConstants;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
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

  LocalDateTime deactivationDate;

}

