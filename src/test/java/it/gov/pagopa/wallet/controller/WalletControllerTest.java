package it.gov.pagopa.wallet.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.EnrollmentStatusDTO;
import it.gov.pagopa.wallet.dto.ErrorDTO;
import it.gov.pagopa.wallet.dto.IbanBodyDTO;
import it.gov.pagopa.wallet.dto.IbanDTO;
import it.gov.pagopa.wallet.dto.InitiativeDTO;
import it.gov.pagopa.wallet.dto.InitiativeListDTO;
import it.gov.pagopa.wallet.dto.InstrumentBodyDTO;
import it.gov.pagopa.wallet.exception.WalletException;
import it.gov.pagopa.wallet.service.WalletService;
import java.util.ArrayList;
import java.util.List;
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
@WebMvcTest(value = {
    WalletController.class}, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class WalletControllerTest {

  private static final String BASE_URL = "http://localhost:8080/idpay/wallet";
  private static final String USER_ID = "TEST_USER_ID";
  private static final String ENROLL_INSTRUMENT_URL = "/instrument/";
  private static final String ENROLL_IBAN_URL = "/iban/";

  private static final String INITIATIVE_LIST = "/initiative/";
  private static final String STATUS_URL = "/status";
  private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";

  private static final String INITIATIVE_KO = "TEST_INITIATIVE_KO";
  private static final String HPAN = "TEST_HPAN";
  private static final String IBAN_OK = "it99C1234567890123456789012";
  private static final String DESCRIPTION_OK = "conto cointestato";
  private static final String CHANNEL_OK = "APP-IO";
  private static final String HOLDER_BANK_OK = "Unicredit";
  private static final InitiativeDTO INITIATIVE_DTO_TEST = new InitiativeDTO(INITIATIVE_ID,
      INITIATIVE_ID, WalletConstants.STATUS_NOT_REFUNDABLE, null, "TEST_DATE", null, "TEST_AMOUNT",
      null, null);
  private static final InstrumentBodyDTO INSTRUMENT_BODY_DTO =
      new InstrumentBodyDTO(INITIATIVE_ID, HPAN);
  private static final IbanBodyDTO IBAN_BODY_DTO =
      new IbanBodyDTO(INITIATIVE_ID, IBAN_OK, DESCRIPTION_OK);

  private static final IbanBodyDTO IBAN_BODY_DTO_EMPTY = new IbanBodyDTO("", "", "");
  private static final InstrumentBodyDTO INSTRUMENT_BODY_DTO_EMPTY = new InstrumentBodyDTO("", "");
  private static final EnrollmentStatusDTO ENROLLMENT_STATUS_DTO =
      new EnrollmentStatusDTO(WalletConstants.STATUS_NOT_REFUNDABLE);
  private static final InitiativeDTO INITIATIVE_DTO =
      new InitiativeDTO(
          INITIATIVE_ID,
          INITIATIVE_ID,
          WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_IBAN,
          IBAN_OK,
          "",
          "1",
          "450.00",
          "50.00",
          "0.00");

  @MockBean
  WalletService walletServiceMock;

  @Autowired
  protected MockMvc mvc;

  @Autowired
  ObjectMapper objectMapper;

  @Test
  void enroll_instrument_ok() throws Exception {

    Mockito.doNothing().when(walletServiceMock).checkInitiative(INITIATIVE_ID);
    Mockito.doNothing().when(walletServiceMock).enrollInstrument(INITIATIVE_ID, USER_ID, HPAN);

    mvc.perform(MockMvcRequestBuilders.put(BASE_URL + ENROLL_INSTRUMENT_URL + USER_ID)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(INSTRUMENT_BODY_DTO))
            .accept(MediaType.APPLICATION_JSON_VALUE)).andExpect(MockMvcResultMatchers.status().isOk())
        .andReturn();
  }

  @Test
  void enroll_instrument_initiative_ko() throws Exception {

    Mockito.doThrow(
            new WalletException(HttpStatus.FORBIDDEN.value(), WalletConstants.ERROR_INITIATIVE_KO))
        .when(walletServiceMock).checkInitiative(INITIATIVE_ID);

    MvcResult res = mvc.perform(
            MockMvcRequestBuilders.put(BASE_URL + ENROLL_INSTRUMENT_URL + USER_ID)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(INSTRUMENT_BODY_DTO))
                .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(MockMvcResultMatchers.status().isForbidden()).andReturn();

    ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);

    assertEquals(HttpStatus.FORBIDDEN.value(), error.getCode());
    assertEquals(WalletConstants.ERROR_INITIATIVE_KO, error.getMessage());
  }

  @Test
  void enroll_initiative_wallet_not_found() throws Exception {

    Mockito.doNothing().when(walletServiceMock).checkInitiative(INITIATIVE_ID);

    Mockito.doThrow(
            new WalletException(HttpStatus.NOT_FOUND.value(), WalletConstants.ERROR_WALLET_NOT_FOUND))
        .when(walletServiceMock).enrollInstrument(INITIATIVE_ID, USER_ID, HPAN);

    MvcResult res = mvc.perform(
            MockMvcRequestBuilders.put(BASE_URL + ENROLL_INSTRUMENT_URL + USER_ID)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(INSTRUMENT_BODY_DTO))
                .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(MockMvcResultMatchers.status().isNotFound()).andReturn();

    ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);

    assertEquals(HttpStatus.NOT_FOUND.value(), error.getCode());
    assertEquals(WalletConstants.ERROR_WALLET_NOT_FOUND, error.getMessage());
  }

  @Test
  void enroll_instrument_empty_body() throws Exception {

    MvcResult res = mvc.perform(
            MockMvcRequestBuilders.put(BASE_URL + ENROLL_INSTRUMENT_URL + USER_ID)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(INSTRUMENT_BODY_DTO_EMPTY))
                .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(MockMvcResultMatchers.status().isBadRequest()).andReturn();

    ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);

    assertEquals(HttpStatus.BAD_REQUEST.value(), error.getCode());
    assertTrue(error.getMessage().contains(WalletConstants.ERROR_MANDATORY_FIELD));
  }

  @Test
  void status_ok() throws Exception {

    Mockito.when(walletServiceMock.getEnrollmentStatus(INITIATIVE_ID, USER_ID))
        .thenReturn(ENROLLMENT_STATUS_DTO);

    MvcResult res = mvc.perform(
            MockMvcRequestBuilders.get(BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + STATUS_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(INSTRUMENT_BODY_DTO))
                .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

    EnrollmentStatusDTO statusDTO = objectMapper.readValue(res.getResponse().getContentAsString(),
        EnrollmentStatusDTO.class);
    assertEquals(WalletConstants.STATUS_NOT_REFUNDABLE, statusDTO.getStatus());
  }

  @Test
  void status_not_found() throws Exception {

    Mockito.doThrow(
            new WalletException(HttpStatus.NOT_FOUND.value(), WalletConstants.ERROR_WALLET_NOT_FOUND))
        .when(walletServiceMock).getEnrollmentStatus(INITIATIVE_ID, USER_ID);

    MvcResult res = mvc.perform(
            MockMvcRequestBuilders.get(BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + STATUS_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(INSTRUMENT_BODY_DTO))
                .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(MockMvcResultMatchers.status().isNotFound()).andReturn();

    ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);

    assertEquals(HttpStatus.NOT_FOUND.value(), error.getCode());
    assertEquals(WalletConstants.ERROR_WALLET_NOT_FOUND, error.getMessage());
  }

  @Test
  void enroll_iban_wallet_not_found() throws Exception {

    Mockito.doNothing().when(walletServiceMock).checkInitiative(INITIATIVE_ID);

    Mockito.doThrow(
            new WalletException(
                HttpStatus.NOT_FOUND.value(), WalletConstants.ERROR_WALLET_NOT_FOUND))
        .when(walletServiceMock)
        .enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, DESCRIPTION_OK);

    MvcResult res =
        mvc.perform(
                MockMvcRequestBuilders.put(BASE_URL + ENROLL_IBAN_URL + USER_ID)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(objectMapper.writeValueAsString(IBAN_BODY_DTO))
                    .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andReturn();

    ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);

    assertEquals(HttpStatus.NOT_FOUND.value(), error.getCode());
    assertEquals(WalletConstants.ERROR_WALLET_NOT_FOUND, error.getMessage());
  }

  @Test
  void enroll_iban_empty_body() throws Exception {
    MvcResult res =
        mvc.perform(
                MockMvcRequestBuilders.put(BASE_URL + ENROLL_IBAN_URL + USER_ID)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(objectMapper.writeValueAsString(IBAN_BODY_DTO_EMPTY))
                    .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andReturn();

    ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);

    assertEquals(HttpStatus.BAD_REQUEST.value(), error.getCode());
    assertTrue(error.getMessage().contains(WalletConstants.ERROR_MANDATORY_FIELD));
  }

  @Test
  void enroll_iban_initiative_ko() throws Exception {

    Mockito.doThrow(
            new WalletException(HttpStatus.FORBIDDEN.value(), WalletConstants.ERROR_INITIATIVE_KO))
        .when(walletServiceMock)
        .checkInitiative(INITIATIVE_ID);

    MvcResult res =
        mvc.perform(
                MockMvcRequestBuilders.put(BASE_URL + ENROLL_IBAN_URL + USER_ID)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(objectMapper.writeValueAsString(IBAN_BODY_DTO))
                    .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isForbidden())
            .andReturn();

    ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);

    assertEquals(HttpStatus.FORBIDDEN.value(), error.getCode());
    assertEquals(WalletConstants.ERROR_INITIATIVE_KO, error.getMessage());
  }

  @Test
  void enroll_iban_ok() throws Exception {

    Mockito.doNothing().when(walletServiceMock).checkInitiative(INITIATIVE_ID);
    Mockito.doNothing()
        .when(walletServiceMock)
        .enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, DESCRIPTION_OK);

    mvc.perform(
            MockMvcRequestBuilders.put(BASE_URL + ENROLL_IBAN_URL + USER_ID)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(IBAN_BODY_DTO))
                .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(MockMvcResultMatchers.status().isNoContent())
        .andReturn();
  }
  
  @Test
  void initiativeList() throws Exception {
    InitiativeListDTO initiativeListDTO = new InitiativeListDTO();

    List<InitiativeDTO> initiativeDTOList = new ArrayList<>();
    initiativeDTOList.add(INITIATIVE_DTO_TEST);
    initiativeListDTO.setInitiativeList(initiativeDTOList);

    Mockito.when(walletServiceMock.getInitiativeList(USER_ID)).thenReturn(initiativeListDTO);

    mvc.perform(MockMvcRequestBuilders.get(BASE_URL + INITIATIVE_LIST + USER_ID)
            .contentType(MediaType.APPLICATION_JSON_VALUE).accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
  }

  @Test
  void detail_ok() throws Exception {

    Mockito.when(walletServiceMock.getWalletDetail(INITIATIVE_ID, USER_ID))
        .thenReturn(INITIATIVE_DTO);

    MvcResult res =
        mvc.perform(
                MockMvcRequestBuilders.get(BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

    InitiativeDTO initiativeDTO =
        objectMapper.readValue(res.getResponse().getContentAsString(), InitiativeDTO.class);
    assertEquals(INITIATIVE_DTO.getInitiativeId(), initiativeDTO.getInitiativeId());
    assertEquals(INITIATIVE_DTO.getInitiativeName(), initiativeDTO.getInitiativeName());
    assertEquals(INITIATIVE_DTO.getStatus(), initiativeDTO.getStatus());
    assertEquals(INITIATIVE_DTO.getEndDate(), initiativeDTO.getEndDate());
    assertEquals(INITIATIVE_DTO.getIban(), initiativeDTO.getIban());
    assertEquals(INITIATIVE_DTO.getNInstr(), initiativeDTO.getNInstr());
    assertEquals(INITIATIVE_DTO.getAmount(), initiativeDTO.getAmount());
    assertEquals(INITIATIVE_DTO.getAccrued(), initiativeDTO.getAccrued());
    assertEquals(INITIATIVE_DTO.getRefunded(), initiativeDTO.getRefunded());
  }

  @Test
  void detail_not_found() throws Exception {

    Mockito.doThrow(
            new WalletException(
                HttpStatus.NOT_FOUND.value(), WalletConstants.ERROR_WALLET_NOT_FOUND))
        .when(walletServiceMock)
        .getWalletDetail(INITIATIVE_ID, USER_ID);

    MvcResult res =
        mvc.perform(
                MockMvcRequestBuilders.get(BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andReturn();

    ErrorDTO error =
        objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);
    assertEquals(HttpStatus.NOT_FOUND.value(), error.getCode());
    assertEquals(WalletConstants.ERROR_WALLET_NOT_FOUND, error.getMessage());
  }

  @Test
  void getIban_ok() throws Exception {
    IbanDTO ibanDTO = new IbanDTO(IBAN_OK,DESCRIPTION_OK,HOLDER_BANK_OK, CHANNEL_OK);

    Mockito.when(walletServiceMock.getIban(INITIATIVE_ID, USER_ID))
        .thenReturn(ibanDTO);

    mvc.perform(
            MockMvcRequestBuilders.get(BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID+ "/iban")
                .contentType(MediaType.APPLICATION_JSON_VALUE).accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
  }

  @Test
  void getIban_ko() throws Exception {

    Mockito.doThrow(new WalletException(HttpStatus.NOT_FOUND.value(),
            String.format("Iban for initiativeId %s and userId %s not found.", INITIATIVE_KO,
                USER_ID)))
        .when(walletServiceMock).getIban(INITIATIVE_KO, USER_ID);

    mvc.perform(
            MockMvcRequestBuilders.get(BASE_URL + "/" + INITIATIVE_KO + "/" + USER_ID)
                .contentType(MediaType.APPLICATION_JSON_VALUE).accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isNotFound()).andReturn();
  }
}
