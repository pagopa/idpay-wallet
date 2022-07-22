package it.gov.pagopa.wallet.dto;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class InitiativeListDTO {

  List<InitiativeDTO> initiativeList;

}
