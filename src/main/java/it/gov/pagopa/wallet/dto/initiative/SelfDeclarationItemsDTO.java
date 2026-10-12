package it.gov.pagopa.wallet.dto.initiative;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "_type",
    visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = SelfCriteriaMultiDTO.class, name = "multi"),
    @JsonSubTypes.Type(value = SelfCriteriaBoolDTO.class, name = "boolean"),
    @JsonSubTypes.Type(value= SelfCriteriaInformativeDTO.class, name = "informative")
})
public interface SelfDeclarationItemsDTO {

}
