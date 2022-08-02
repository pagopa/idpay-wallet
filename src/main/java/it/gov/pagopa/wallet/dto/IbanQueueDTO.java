package it.gov.pagopa.wallet.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class IbanQueueDTO {

  String userId;

  String iban;

  String description;

  String channel;

  LocalDateTime queueDate;
}
