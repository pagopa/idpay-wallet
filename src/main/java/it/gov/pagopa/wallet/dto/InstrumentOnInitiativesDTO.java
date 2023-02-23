package it.gov.pagopa.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class InstrumentOnInitiativesDTO {

    String idWallet;
    String idInstrument;
    String maskedPan;
    String brandLogo;
    String brand;
    List<InstrumentStatusOnInitiativeDTO> initiativeList;

}
