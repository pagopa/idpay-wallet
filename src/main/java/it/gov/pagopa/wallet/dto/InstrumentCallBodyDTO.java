package it.gov.pagopa.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InstrumentCallBodyDTO {

  String userId;

  String initiativeId;

  String idWallet;

  String channel;

  String instrumentType;

}

