package it.gov.pagopa.wallet.dto.initiative;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ChannelDTO {

  @JsonProperty("type")
  private String type;

  @JsonProperty("contact")
  private String contact;
}
