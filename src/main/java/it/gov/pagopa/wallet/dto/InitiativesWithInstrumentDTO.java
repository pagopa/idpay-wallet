package it.gov.pagopa.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class InitiativesWithInstrumentDTO {

    String idWallet;
    String maskedPan;
    String brand;
    List<InitiativesStatusDTO> initiativeList;

}
