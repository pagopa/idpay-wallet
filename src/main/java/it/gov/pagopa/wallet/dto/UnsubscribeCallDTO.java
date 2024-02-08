package it.gov.pagopa.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UnsubscribeCallDTO {

  private String initiativeId;
  private String userId;
  private String unsubscribeDate;
  private String channel;
}
