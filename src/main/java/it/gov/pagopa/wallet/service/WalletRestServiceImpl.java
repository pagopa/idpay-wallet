package it.gov.pagopa.wallet.service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.gov.pagopa.wallet.dto.IbanCallBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentCallBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
@Service
public class WalletRestServiceImpl implements WalletRestService {

  @Autowired
  Environment env;
  private static final String ENROLL_INSTRUMENT = "/idpay/instrument/enroll";
  private static final String ENROLL_IBAN = "/idpay/iban/enroll";
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
        .exchange(env.getProperty("payment.instrument.uri") + ENROLL_INSTRUMENT, HttpMethod.PUT, requestEntity, InstrumentResponseDTO.class)
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
        .exchange(env.getProperty("iban.uri") + ENROLL_IBAN, HttpMethod.PUT, requestEntity, Void.class);
  }
}