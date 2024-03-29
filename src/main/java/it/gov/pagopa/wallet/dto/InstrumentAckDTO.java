package it.gov.pagopa.wallet.dto;

import it.gov.pagopa.wallet.constants.WalletConstants;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
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

  String instrumentType; //TODO 1802 mandatory field?

  String brandLogo;

  String brand;

  String maskedPan;

  @NotBlank(message = WalletConstants.ERROR_MANDATORY_FIELD)
  String operationType;

  LocalDateTime operationDate;

  @Min(value = 0, message = WalletConstants.ERROR_LESS_THAN_ZERO)
  Integer ninstr;



}
