package it.gov.pagopa.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WalletPIDTO {

  private String initiativeId;
  private String userId;
  private String maskedPan;
  private String brandLogo;
  private String brand;
}
