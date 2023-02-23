package it.gov.pagopa.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class InstrumentDetailDTO {

    String idInstrument;
    String maskedPan;
    String brandLogo;
    String brand;
    List<StatusOnInitiativeDTO> initiativeList;

}
