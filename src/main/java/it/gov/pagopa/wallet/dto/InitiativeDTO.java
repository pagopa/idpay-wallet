package it.gov.pagopa.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InitiativeDTO {

  private String initiativeId;

  private String initiativeName;

  private String status;

  private String iban;

  private String endDate;

  private String nInstr;

  private String amount;

  private String accrued;

  private String refunded;

}
