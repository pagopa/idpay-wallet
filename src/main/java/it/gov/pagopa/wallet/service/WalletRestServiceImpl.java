package it.gov.pagopa.wallet.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.gov.pagopa.wallet.dto.IbanCallBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentCallBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentResponseDTO;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WalletRestServiceImpl implements WalletRestService {
  private static final String ENROLL_INSTRUMENT = "http://localhost:8080/idpay/instrument/enroll";
  private static final String ENROLL_IBAN = "http://localhost:8080/idpay/iban/enroll";

  private RestTemplate restTemplate = new RestTemplate();

  @Override
  public InstrumentResponseDTO callPaymentInstrument(
      InstrumentCallBodyDTO dto) throws JsonProcessingException {

    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<String> requestEntity;

    requestEntity = new HttpEntity<>(objectMapper.writeValueAsString(dto), headers);

    return restTemplate
        .exchange(ENROLL_INSTRUMENT, HttpMethod.PUT, requestEntity, InstrumentResponseDTO.class)
        .getBody();
  }

  @Override
  public void callIban(IbanCallBodyDTO dto) throws JsonProcessingException {

      ObjectMapper objectMapper = new ObjectMapper();

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      HttpEntity<String> requestEntity;

      requestEntity = new HttpEntity<>(objectMapper.writeValueAsString(dto), headers);

      restTemplate
              .exchange(ENROLL_IBAN, HttpMethod.PUT, requestEntity, Void.class);

  }
}
