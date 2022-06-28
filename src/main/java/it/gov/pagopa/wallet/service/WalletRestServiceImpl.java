package it.gov.pagopa.wallet.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.wallet.dto.InstrumentCallBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentResponseDTO;
import it.gov.pagopa.wallet.exception.WalletException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WalletRestServiceImpl implements WalletRestService {

  @Autowired ObjectMapper objectMapper;
  private static final String ENROLL_URI = "http://localhost:8080/idpay/instrument/enroll";

  @Override
  public InstrumentResponseDTO callPaymentInstrument(
      InstrumentCallBodyDTO dto) {

    RestTemplate restTemplate = new RestTemplate();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<String> requestEntity;

    try {
      requestEntity = new HttpEntity<>(objectMapper.writeValueAsString(dto), headers);
    } catch (JsonProcessingException jpe) {
      throw new WalletException(HttpStatus.BAD_REQUEST.value(), jpe.getMessage());
    }
    return restTemplate
        .exchange(ENROLL_URI, HttpMethod.PUT, requestEntity, InstrumentResponseDTO.class)
        .getBody();
  }
}
