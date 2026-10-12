package it.gov.pagopa.wallet.dto.initiative;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * SelfCriteriaMultiConsentDTO
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SelfCriteriaMultiConsentDTO implements SelfDeclarationItemsDTO {

  @JsonProperty("_type")
  private String type;

  @JsonProperty("description")
  private String description;

  @JsonProperty("subDescription")
  private String subDescription;

  @JsonProperty("value")
  private List<SelfCriteriaMultiConsentValueDTO> value;

  @JsonProperty("code")
  private String code;
}
