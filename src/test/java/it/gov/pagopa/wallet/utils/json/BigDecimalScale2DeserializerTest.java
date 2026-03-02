package it.gov.pagopa.wallet.utils.json;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BigDecimalScale2DeserializerTest {

    private JsonMapper mapperWithDeserializer() {
        SimpleModule m = new SimpleModule();
        m.addDeserializer(BigDecimal.class, new BigDecimalScale2Deserializer());
        return JsonMapper.builder().addModule(m).build();
    }

    @Test
    void deserialize_moreThanTwoDecimals_roundsCorrectly() throws Exception {
        JsonMapper mapper = mapperWithDeserializer();
        BigDecimal result = mapper.readValue("1.236", BigDecimal.class);

        assertEquals(2, result.scale());
        assertEquals(0, result.compareTo(new BigDecimal("1.24")));
    }

    @Test
    void deserialize_halfDown_tieRoundsDown() throws Exception {
        JsonMapper mapper = mapperWithDeserializer();
        BigDecimal result = mapper.readValue("1.235", BigDecimal.class);

        assertEquals(2, result.scale());
        assertEquals(0, result.compareTo(new BigDecimal("1.23")));
    }

    @Test
    void deserialize_integer_getsTwoDecimalScale() throws Exception {
        JsonMapper mapper = mapperWithDeserializer();
        BigDecimal result = mapper.readValue("2", BigDecimal.class);

        assertEquals(2, result.scale());
        assertEquals(0, result.compareTo(new BigDecimal("2.00")));
    }

    @Test
    void deserialize_negativeHalfDown_tieRoundsTowardZero() throws Exception {
        JsonMapper mapper = mapperWithDeserializer();
        BigDecimal result = mapper.readValue("-1.235", BigDecimal.class);

        assertEquals(2, result.scale());
        assertEquals(0, result.compareTo(new BigDecimal("-1.23")));
    }
}
