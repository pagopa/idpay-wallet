package it.gov.pagopa.wallet.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiativeListDTO {

  List<InitiativeDTO> initiativeList;

}
