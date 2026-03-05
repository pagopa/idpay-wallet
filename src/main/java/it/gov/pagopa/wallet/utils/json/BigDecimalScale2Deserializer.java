package it.gov.pagopa.wallet.utils.json;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BigDecimalScale2Deserializer extends StdDeserializer<BigDecimal> {
  public BigDecimalScale2Deserializer() {
    super(BigDecimal.class);
  }

  @Override
  public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) {
    return p.getDecimalValue().setScale(2, RoundingMode.HALF_DOWN);
  }
}
