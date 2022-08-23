package it.gov.pagopa.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IbanQueueWalletDTO {
  private String userId;
  private String iban;
  private String status;
  private String queueDate;
}
