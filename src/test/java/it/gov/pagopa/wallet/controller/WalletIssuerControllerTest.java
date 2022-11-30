package it.gov.pagopa.wallet.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.EnrollmentStatusDTO;
import it.gov.pagopa.wallet.dto.ErrorDTO;
import it.gov.pagopa.wallet.dto.IbanBodyDTO;
import it.gov.pagopa.wallet.dto.InitiativeListDTO;
import it.gov.pagopa.wallet.dto.InstrumentAckDTO;
import it.gov.pagopa.wallet.dto.WalletDTO;
import it.gov.pagopa.wallet.dto.WalletPIBodyDTO;
import it.gov.pagopa.wallet.dto.WalletPIDTO;
import it.gov.pagopa.wallet.enums.WalletStatus;
import it.gov.pagopa.wallet.exception.WalletException;
import it.gov.pagopa.wallet.service.WalletService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.iban4j.IbanFormatException;
import org.iban4j.InvalidCheckDigitException;
import org.iban4j.UnsupportedCountryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(
    value = {WalletIssuerController.class},
    excludeAutoConfiguration = SecurityAutoConfiguration.class)
class WalletIssuerControllerTest {

  private static final String BASE_URL = "http://localhost:8080/idpay/hb/wallet";
  private static final String USER_ID = "TEST_USER_ID";
  private static final String STATUS_URL = "/status";
  private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";
  private static final EnrollmentStatusDTO ENROLLMENT_STATUS_DTO =
      new EnrollmentStatusDTO(WalletStatus.NOT_REFUNDABLE.name());

  @MockBean WalletService walletServiceMock;

  @Autowired protected MockMvc mvc;

  @Autowired ObjectMapper objectMapper;

  @Test
  void status_ok() throws Exception {

    Mockito.when(walletServiceMock.getEnrollmentStatus(INITIATIVE_ID, USER_ID))
        .thenReturn(ENROLLMENT_STATUS_DTO);

    MvcResult res =
        mvc.perform(
                MockMvcRequestBuilders.get(
                        BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + STATUS_URL)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    EnrollmentStatusDTO statusDTO =
        objectMapper.readValue(res.getResponse().getContentAsString(), EnrollmentStatusDTO.class);
    assertEquals(WalletStatus.NOT_REFUNDABLE.name(), statusDTO.getStatus());
  }

  @Test
  void status_not_found() throws Exception {

    Mockito.doThrow(
            new WalletException(
                HttpStatus.NOT_FOUND.value(), WalletConstants.ERROR_WALLET_NOT_FOUND))
        .when(walletServiceMock)
        .getEnrollmentStatus(INITIATIVE_ID, USER_ID);

    MvcResult res =
        mvc.perform(
                MockMvcRequestBuilders.get(
                        BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + STATUS_URL)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andReturn();

    ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);

    assertEquals(HttpStatus.NOT_FOUND.value(), error.getCode());
    assertEquals(WalletConstants.ERROR_WALLET_NOT_FOUND, error.getMessage());
  }

}
