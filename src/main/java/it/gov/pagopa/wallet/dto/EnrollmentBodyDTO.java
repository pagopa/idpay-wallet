package it.gov.pagopa.wallet.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EnrollmentBodyDTO {

  String userId;

  String initiativeId;

  String hpan;

  String channel;

  LocalDateTime activationDate;

}

