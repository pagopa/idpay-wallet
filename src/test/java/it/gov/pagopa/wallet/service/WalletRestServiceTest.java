package it.gov.pagopa.wallet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.InstrumentCallBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentResponseDTO;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(value = {WalletRestService.class})
class WalletRestServiceTest {

  private static final String ENROLL_INSTRUMENT_URI = "http://localhost:8080/idpay/instrument/enroll";
  private static final String ENROLL_IBAN_URI = "http://localhost:8080/idpay/iban/enroll";
  private static final String USER_ID = "TEST_USER_ID";
  private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";
  private static final String HPAN = "TEST_HPAN";
  private static final String IBAN_OK = "it99C1234567890123456789012";
  private static final String DESCRIPTION_OK = "conto cointestato";
  private static final LocalDateTime TEST_DATE = LocalDateTime.now();
  private static final int TEST_COUNT = 2;
  private static final InstrumentResponseDTO INSTRUMENT_RESPONSE_DTO =
      new InstrumentResponseDTO(TEST_COUNT);
  private static final InstrumentCallBodyDTO INSTRUMENT_CALL_BODY_DTO = new InstrumentCallBodyDTO(
      USER_ID, INITIATIVE_ID, HPAN,
      WalletConstants.CHANNEL_APP_IO, TEST_DATE);
  @Mock
  RestTemplate restTemplate;

  @Mock
  Environment env;
  @InjectMocks
  WalletRestServiceImpl walletRestService;

  @Test
  void callPaymentInstrument_ok() throws Exception {
    Mockito.when(env.getProperty("payment.instrument.uri")).thenReturn("http://localhost:8080");
    Mockito.when(
            restTemplate.exchange(
                Mockito.eq(ENROLL_INSTRUMENT_URI),
                Mockito.eq(HttpMethod.PUT),
                Mockito.any(HttpEntity.class),
                Mockito.eq(InstrumentResponseDTO.class)))
        .thenReturn(new ResponseEntity<>(INSTRUMENT_RESPONSE_DTO, HttpStatus.OK));

    InstrumentResponseDTO responseDTO = walletRestService.callPaymentInstrument(
        INSTRUMENT_CALL_BODY_DTO);

    assertNotNull(responseDTO);
    assertEquals(TEST_COUNT, responseDTO.getNinstr());
  }
}
