package it.gov.pagopa.wallet.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class WalletPIBodyDTO {

  private List<WalletPIDTO> walletDTOlist;
}
