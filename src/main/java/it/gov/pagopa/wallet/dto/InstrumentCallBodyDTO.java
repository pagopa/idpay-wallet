package it.gov.pagopa.wallet.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class InstrumentCallBodyDTO {

  String userId;

  String initiativeId;

  String idWallet;

  String channel;

  LocalDateTime activationDate;

}

