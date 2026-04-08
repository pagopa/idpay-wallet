package it.gov.pagopa.wallet.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class IbanQueueDTO {

  String userId;

  String initiativeId;

  String iban;

  String description;

  String channel;

  Instant queueDate;
}
