package it.gov.pagopa.wallet.dto.initiative;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class SelfCriteriaMultiDTO implements SelfDeclarationItemsDTO {

  @JsonProperty("_type")
  private String type;

  @JsonProperty("description")
  private String description;

  @JsonProperty("value")
  private List<String> value;

  @JsonProperty("code")
  private String code;

  @JsonIgnore
  public String getFirst(){
    return value.get(0);
  }

}
