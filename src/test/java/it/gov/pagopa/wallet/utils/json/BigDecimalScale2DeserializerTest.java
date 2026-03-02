package it.gov.pagopa.wallet.utils.json;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    @ParameterizedTest
    @CsvSource({
            "1.236,1.24",
            "1.235,1.23",
            "2,2.00",
            "-1.235,-1.23"
    })
    void deserialize_various_rounding(String input, String expected) throws Exception {
        JsonMapper mapper = mapperWithDeserializer();
        BigDecimal result = mapper.readValue(input, BigDecimal.class);

        assertEquals(2, result.scale());
        assertEquals(0, result.compareTo(new BigDecimal(expected)));
    }
}
