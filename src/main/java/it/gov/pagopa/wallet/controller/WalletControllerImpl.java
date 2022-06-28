package it.gov.pagopa.wallet.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.EnrollmentBodyDTO;
import it.gov.pagopa.wallet.dto.EnrollmentStatusDTO;
import it.gov.pagopa.wallet.dto.InstrumentBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentResponseDTO;
import it.gov.pagopa.wallet.exception.WalletException;
import it.gov.pagopa.wallet.model.Wallet;
import it.gov.pagopa.wallet.service.WalletService;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@RestController
public class WalletControllerImpl implements WalletController {

  private static final String ENROLL_URI = "http://localhost:8080/idpay/instrument/enroll";

  @Autowired WalletService walletService;

  @Autowired ObjectMapper objectMapper;

  @Override
  public ResponseEntity<Void> enrollInstrument(InstrumentBodyDTO body, String userId) {
    walletService.checkInitiative(body.getInitiativeId());

    Wallet wallet = walletService.findByInitiativeIdAndUserId(body.getInitiativeId(), userId);

    RestTemplate restTemplate = new RestTemplate();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    EnrollmentBodyDTO dto =
        new EnrollmentBodyDTO(
            userId,
            body.getInitiativeId(),
            body.getHpan(),
            WalletConstants.CHANNEL_APP_IO,
            LocalDateTime.now());
    HttpEntity<String> requestEntity;

    ResponseEntity<InstrumentResponseDTO> response;
    try {
      requestEntity = new HttpEntity<>(objectMapper.writeValueAsString(dto), headers);
      response = restTemplate.exchange(ENROLL_URI, HttpMethod.PUT, requestEntity, InstrumentResponseDTO.class);
      walletService.updateEnrollmentWithNewInstrument(wallet,
          Objects.requireNonNull(response.getBody()).getNinstr());
    } catch (JsonProcessingException jpe) {
      throw new WalletException(HttpStatus.BAD_REQUEST.value(), jpe.getMessage());
    } catch (HttpClientErrorException e) {
      throw new WalletException(e.getRawStatusCode(), e.getMessage());
    }

    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Override
  public ResponseEntity<EnrollmentStatusDTO> enrollmentStatus(String initiativeId, String userId) {
    EnrollmentStatusDTO enrollmentStatusDTO =
        walletService.getEnrollmentStatus(initiativeId, userId);
    return new ResponseEntity<>(enrollmentStatusDTO, HttpStatus.OK);
  }
}
