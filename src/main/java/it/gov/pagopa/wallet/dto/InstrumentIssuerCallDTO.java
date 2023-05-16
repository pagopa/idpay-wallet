package it.gov.pagopa.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class InstrumentIssuerCallDTO {

  String initiativeId;

  String userId;

  String hpan;

  String channel;

  String brandLogo;

  String brand;

  String maskedPan;
}
