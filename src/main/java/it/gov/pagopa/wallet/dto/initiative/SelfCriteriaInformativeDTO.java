package it.gov.pagopa.wallet.dto.initiative;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

// BND-1882 / BND-1883: informative self declaration criteria (dto side) - codes ANPR and ADE

/**
 * SelfCriteriaInformativeDTO
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SelfCriteriaInformativeDTO implements SelfDeclarationItemsDTO {

  @JsonProperty("_type")
  private String type;

  @JsonProperty("code")
  private String code;

  @JsonProperty("description")
  private String description;

  @JsonProperty("organization")
  private String organization;

  @JsonProperty("value")
  private String value;

}


