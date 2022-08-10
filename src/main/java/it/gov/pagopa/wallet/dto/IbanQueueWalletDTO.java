package it.gov.pagopa.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IbanQueueWalletDTO {
  private String userId;
  private String iban;
  private String status;
  private String errorCode;
  private String errorDescription;
  private String queueDate;

}
