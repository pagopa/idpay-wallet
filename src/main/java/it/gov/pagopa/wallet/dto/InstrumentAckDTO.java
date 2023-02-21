package it.gov.pagopa.wallet.dto;

import it.gov.pagopa.wallet.constants.WalletConstants;
import java.time.LocalDateTime;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class InstrumentAckDTO {

  @NotBlank(message = WalletConstants.ERROR_MANDATORY_FIELD)
  String initiativeId;

  @NotBlank(message = WalletConstants.ERROR_MANDATORY_FIELD)
  String userId;

  @NotBlank(message = WalletConstants.ERROR_MANDATORY_FIELD)
  String channel;

  String brandLogo;

  String brand;

  String circuitType;

  String maskedPan;

  @NotBlank(message = WalletConstants.ERROR_MANDATORY_FIELD)
  String operationType;

  LocalDateTime operationDate;

  @Min(value = 0, message = WalletConstants.ERROR_LESS_THAN_ZERO)
  Integer ninstr;



}
