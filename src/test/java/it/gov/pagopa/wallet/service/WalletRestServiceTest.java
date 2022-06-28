package it.gov.pagopa.wallet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.InstrumentCallBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentResponseDTO;
import it.gov.pagopa.wallet.exception.WalletException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(value = {WalletRestService.class})
class WalletRestServiceTest {

  @Mock RestTemplate restTemplate;

  @InjectMocks WalletRestServiceImpl walletRestService;

  private static final String ENROLL_URI = "http://localhost:8080/idpay/instrument/enroll";
  private static final String USER_ID = "TEST_USER_ID";
  private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";
  private static final String HPAN = "TEST_HPAN";
  private static final LocalDateTime TEST_DATE = LocalDateTime.now();
  private static final int TEST_COUNT = 2;

  private static final InstrumentResponseDTO INSTRUMENT_RESPONSE_DTO =
      new InstrumentResponseDTO(TEST_COUNT);

  private static final InstrumentCallBodyDTO INSTRUMENT_CALL_BODY_DTO = new InstrumentCallBodyDTO(USER_ID, INITIATIVE_ID, HPAN,
      WalletConstants.CHANNEL_APP_IO, TEST_DATE);

  @Test
  void callPaymentInstrument_ok() throws Exception{
    Mockito.when(
            restTemplate.exchange(
                Mockito.eq(ENROLL_URI),
                Mockito.eq(HttpMethod.PUT),
                Mockito.any(HttpEntity.class),
                Mockito.eq(InstrumentResponseDTO.class)))
        .thenReturn(new ResponseEntity<>(INSTRUMENT_RESPONSE_DTO, HttpStatus.OK));

    InstrumentResponseDTO responseDTO = walletRestService.callPaymentInstrument(INSTRUMENT_CALL_BODY_DTO);

    assertNotNull(responseDTO);
    assertEquals(TEST_COUNT, responseDTO.getNinstr());
  }
}
