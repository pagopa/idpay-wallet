package it.gov.pagopa.wallet.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.*;
import it.gov.pagopa.wallet.enums.WalletStatus;
import it.gov.pagopa.wallet.exception.WalletException;
import it.gov.pagopa.wallet.service.WalletService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import it.gov.pagopa.wallet.utils.Utilities;
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
        value = {WalletController.class},
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class WalletControllerTest {

    private static final String BASE_URL = "http://localhost:8080/idpay/wallet";
    private static final String USER_ID = "TEST_USER_ID";
    private static final String FAMILY_ID = "TEST_FAMILY_ID";
    private static final String ENROLL_INSTRUMENT_URL = "/instruments";
    private static final String INSTRUMENTS_URL = "/instruments/";
    private static final String ENROLL_IBAN_URL = "/iban/";
    private static final String STATUS_URL = "/status";
    private static final String UNSUBSCRIBE_URL = "/unsubscribe";
    private static final String UPDATE_WALLET_URL = "/updateWallet";
    private static final String PROCESS_ACK_URL = "/acknowledge";
    private static final String SUSPEND_URL = "/suspend";
    private static final String READMIT_URL = "/readmit";
    private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";
    private static final String ORGANIZATION_ID = "TEST_ORGANIZATION_ID";
    private static final String ID_WALLET = "TEST_ID_WALLET";
    private static final String INSTRUMENT_ID = "TEST_INSTRUMENT_ID";
    private static final String IBAN_OK = "it99C1234567890123456789012";
    private static final String IBAN_WRONG = "it99C1234567890123456789012222";
    private static final String CHANNEL = "APP_IO";
    private static final String IBAN_WRONG_DIGIT = "IT09P3608105138205493205496";
    private static final String IBAN_KO_NOT_IT = "GB29NWBK60161331926819";
    private static final String DESCRIPTION_OK = "conto cointestato";
    private static final String MASKED_PAN = "masked_pan";
    private static final String BRAND_LOGO = "brand_logo";
    private static final String BRAND = "brand";
    private static final String LOGO_URL = "https://test" + String.format(Utilities.LOGO_PATH_TEMPLATE,
            ORGANIZATION_ID, INITIATIVE_ID, Utilities.LOGO_NAME);
    private static final String ORGANIZATION_NAME = "TEST_ORGANIZATION_NAME";
    private static final String SERVICE_ID = "SERVICE_ID";

    private static final LocalDate DATE = LocalDate.now();
    private static final LocalDateTime TEST_DATE = LocalDateTime.now();

    private static final WalletDTO INITIATIVE_DTO_TEST =
            new WalletDTO(
                    FAMILY_ID,
                    INITIATIVE_ID,
                    INITIATIVE_ID,
                    WalletStatus.NOT_REFUNDABLE.name(),
                    null,
                    DATE,
                    0,
                    null,
                    null,
                    null,
                    TEST_DATE,
                    WalletConstants.INITIATIVE_REWARD_TYPE_REFUND,
                    LOGO_URL,
                    ORGANIZATION_NAME,
                    null,
                    100L,
                    SERVICE_ID);
    private static final IbanBodyDTO IBAN_BODY_DTO =
            new IbanBodyDTO(IBAN_OK, DESCRIPTION_OK, CHANNEL);

    private static final IbanBodyDTO IBAN_BODY_DTO_EMPTY = new IbanBodyDTO("", "", "");
    private static final EnrollmentStatusDTO ENROLLMENT_STATUS_DTO =
            new EnrollmentStatusDTO(WalletStatus.NOT_REFUNDABLE.name());
    private static final WalletDTO INITIATIVE_DTO =
            new WalletDTO(
                    FAMILY_ID,
                    INITIATIVE_ID,
                    INITIATIVE_ID,
                    WalletStatus.NOT_REFUNDABLE_ONLY_IBAN.name(),
                    IBAN_OK,
                    DATE,
                    1,
                    new BigDecimal("450.00"),
                    new BigDecimal("50.00"),
                    new BigDecimal("0.00"),
                    TEST_DATE,
                    WalletConstants.INITIATIVE_REWARD_TYPE_REFUND,
                    LOGO_URL,
                    ORGANIZATION_NAME,
                    10L,
                    100L,
                    SERVICE_ID);
    private static final WalletDTO INITIATIVE_ISSUER_DTO =
            new WalletDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    0,
                    new BigDecimal("450.00"),
                    new BigDecimal("50.00"),
                    new BigDecimal("0.00"),
                    TEST_DATE,
                    WalletConstants.INITIATIVE_REWARD_TYPE_REFUND,
                    LOGO_URL,
                    ORGANIZATION_NAME,
                    null,
                    100L,
                    SERVICE_ID);

    @MockBean
    WalletService walletServiceMock;

    @Autowired
    protected MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void enroll_instrument_ok() throws Exception {

        Mockito.doNothing().when(walletServiceMock).enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET);

        mvc.perform(
                        MockMvcRequestBuilders.put(
                                        BASE_URL
                                                + "/"
                                                + INITIATIVE_ID
                                                + "/"
                                                + USER_ID
                                                + ENROLL_INSTRUMENT_URL
                                                + "/"
                                                + ID_WALLET)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
    }

    @Test
    void enroll_instrument_initiative_ko() throws Exception {

        Mockito.doThrow(
                        new WalletException(HttpStatus.FORBIDDEN.value(), WalletConstants.ERROR_INITIATIVE_KO))
                .when(walletServiceMock)
                .enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET);

        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.put(
                                                BASE_URL
                                                        + "/"
                                                        + INITIATIVE_ID
                                                        + "/"
                                                        + USER_ID
                                                        + ENROLL_INSTRUMENT_URL
                                                        + "/"
                                                        + ID_WALLET)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(MockMvcResultMatchers.status().isForbidden())
                        .andReturn();

        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);

        assertEquals(HttpStatus.FORBIDDEN.value(), error.getCode());
        assertEquals(WalletConstants.ERROR_INITIATIVE_KO, error.getMessage());
    }

    @Test
    void enroll_initiative_wallet_not_found() throws Exception {

        Mockito.doThrow(
                        new WalletException(
                                HttpStatus.NOT_FOUND.value(), WalletConstants.ERROR_WALLET_NOT_FOUND))
                .when(walletServiceMock)
                .enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET);

        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.put(
                                                BASE_URL
                                                        + "/"
                                                        + INITIATIVE_ID
                                                        + "/"
                                                        + USER_ID
                                                        + ENROLL_INSTRUMENT_URL
                                                        + "/"
                                                        + ID_WALLET)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(MockMvcResultMatchers.status().isNotFound())
                        .andReturn();

        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), error.getCode());
        assertEquals(WalletConstants.ERROR_WALLET_NOT_FOUND, error.getMessage());
    }

    @Test
    void delete_instrument_ok() throws Exception {

        Mockito.doNothing()
                .when(walletServiceMock)
                .deleteInstrument(INITIATIVE_ID, USER_ID, INSTRUMENT_ID);

        mvc.perform(
                        MockMvcRequestBuilders.delete(
                                        BASE_URL
                                                + "/"
                                                + INITIATIVE_ID
                                                + "/"
                                                + USER_ID
                                                + INSTRUMENTS_URL
                                                + "/"
                                                + INSTRUMENT_ID)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
    }

    @Test
    void delete_instrument_initiative_ko() throws Exception {

        Mockito.doThrow(
                        new WalletException(HttpStatus.FORBIDDEN.value(), WalletConstants.ERROR_INITIATIVE_KO))
                .when(walletServiceMock)
                .deleteInstrument(INITIATIVE_ID, USER_ID, INSTRUMENT_ID);

        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.delete(
                                                BASE_URL
                                                        + "/"
                                                        + INITIATIVE_ID
                                                        + "/"
                                                        + USER_ID
                                                        + INSTRUMENTS_URL
                                                        + "/"
                                                        + INSTRUMENT_ID)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(MockMvcResultMatchers.status().isForbidden())
                        .andReturn();

        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);

        assertEquals(HttpStatus.FORBIDDEN.value(), error.getCode());
        assertEquals(WalletConstants.ERROR_INITIATIVE_KO, error.getMessage());
    }

    @Test
    void delete_instrument_initiative_wallet_not_found() throws Exception {

        Mockito.doThrow(
                        new WalletException(
                                HttpStatus.NOT_FOUND.value(), WalletConstants.ERROR_WALLET_NOT_FOUND))
                .when(walletServiceMock)
                .deleteInstrument(INITIATIVE_ID, USER_ID, INSTRUMENT_ID);

        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.delete(
                                                BASE_URL
                                                        + "/"
                                                        + INITIATIVE_ID
                                                        + "/"
                                                        + USER_ID
                                                        + INSTRUMENTS_URL
                                                        + "/"
                                                        + INSTRUMENT_ID)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(MockMvcResultMatchers.status().isNotFound())
                        .andReturn();

        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), error.getCode());
        assertEquals(WalletConstants.ERROR_WALLET_NOT_FOUND, error.getMessage());
    }

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

    @Test
    void enroll_iban_wallet_not_found() throws Exception {

        Mockito.doThrow(
                        new WalletException(
                                HttpStatus.NOT_FOUND.value(), WalletConstants.ERROR_WALLET_NOT_FOUND))
                .when(walletServiceMock)
                .enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, CHANNEL, DESCRIPTION_OK);

        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.put(
                                                BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + ENROLL_IBAN_URL)
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
    void enroll_iban_wallet_format() throws Exception {
        final IbanBodyDTO iban = new IbanBodyDTO(IBAN_WRONG, DESCRIPTION_OK, CHANNEL);

        Mockito.doThrow(new IbanFormatException())
                .when(walletServiceMock)
                .enrollIban(INITIATIVE_ID, USER_ID, IBAN_WRONG, CHANNEL, DESCRIPTION_OK);
        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.put(
                                                BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + ENROLL_IBAN_URL)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .content(objectMapper.writeValueAsString(iban))
                                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(MockMvcResultMatchers.status().isBadRequest())
                        .andReturn();

        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);
        assertEquals(HttpStatus.BAD_REQUEST.value(), error.getCode());
    }

    @Test
    void enroll_iban_invalid_digit() throws Exception {
        final IbanBodyDTO iban = new IbanBodyDTO(IBAN_KO_NOT_IT, DESCRIPTION_OK, CHANNEL);

        Mockito.doThrow(new InvalidCheckDigitException())
                .when(walletServiceMock)
                .enrollIban(INITIATIVE_ID, USER_ID, IBAN_KO_NOT_IT, CHANNEL, DESCRIPTION_OK);
        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.put(
                                                BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + ENROLL_IBAN_URL)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .content(objectMapper.writeValueAsString(iban))
                                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(MockMvcResultMatchers.status().isBadRequest())
                        .andReturn();

        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);
        assertEquals(HttpStatus.BAD_REQUEST.value(), error.getCode());
    }

    @Test
    void enroll_iban_not_it() throws Exception {
        final IbanBodyDTO iban = new IbanBodyDTO(IBAN_WRONG_DIGIT, DESCRIPTION_OK, CHANNEL);

        Mockito.doThrow(new UnsupportedCountryException())
                .when(walletServiceMock)
                .enrollIban(INITIATIVE_ID, USER_ID, IBAN_WRONG_DIGIT, CHANNEL, DESCRIPTION_OK);
        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.put(
                                                BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + ENROLL_IBAN_URL)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .content(objectMapper.writeValueAsString(iban))
                                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(MockMvcResultMatchers.status().isBadRequest())
                        .andReturn();

        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);
        assertEquals(HttpStatus.BAD_REQUEST.value(), error.getCode());
    }

    @Test
    void enroll_iban_empty_body() throws Exception {
        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.put(
                                                BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + ENROLL_IBAN_URL)
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
                .enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, CHANNEL, DESCRIPTION_OK);

        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.put(
                                                BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + ENROLL_IBAN_URL)
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
        Mockito.doNothing()
                .when(walletServiceMock)
                .enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, CHANNEL, DESCRIPTION_OK);

        mvc.perform(
                        MockMvcRequestBuilders.put(
                                        BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + ENROLL_IBAN_URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(IBAN_BODY_DTO))
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
    }

    @Test
    void initiativeList() throws Exception {
        InitiativeListDTO initiativeListDTO = new InitiativeListDTO();

        List<WalletDTO> walletDTOList = new ArrayList<>();
        walletDTOList.add(INITIATIVE_DTO_TEST);
        initiativeListDTO.setInitiativeList(walletDTOList);

        Mockito.when(walletServiceMock.getInitiativeList(USER_ID)).thenReturn(initiativeListDTO);

        mvc.perform(
                        MockMvcRequestBuilders.get(BASE_URL + "/" + USER_ID)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
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

        WalletDTO walletDTO =
                objectMapper.readValue(res.getResponse().getContentAsString(), WalletDTO.class);
        assertEquals(INITIATIVE_DTO.getInitiativeId(), walletDTO.getInitiativeId());
        assertEquals(INITIATIVE_DTO.getInitiativeName(), walletDTO.getInitiativeName());
        assertEquals(INITIATIVE_DTO.getStatus(), walletDTO.getStatus());
        assertEquals(INITIATIVE_DTO.getEndDate(), walletDTO.getEndDate());
        assertEquals(INITIATIVE_DTO.getIban(), walletDTO.getIban());
        assertEquals(INITIATIVE_DTO.getNInstr(), walletDTO.getNInstr());
        assertEquals(INITIATIVE_DTO.getAmount(), walletDTO.getAmount());
        assertEquals(INITIATIVE_DTO.getAccrued(), walletDTO.getAccrued());
        assertEquals(INITIATIVE_DTO.getRefunded(), walletDTO.getRefunded());
        assertEquals(INITIATIVE_DTO.getNTrx(), walletDTO.getNTrx());
        assertEquals(INITIATIVE_DTO.getMaxTrx(),walletDTO.getMaxTrx());
        assertEquals(INITIATIVE_DTO.getServiceId(), walletDTO.getServiceId());
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

        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);
        assertEquals(HttpStatus.NOT_FOUND.value(), error.getCode());
        assertEquals(WalletConstants.ERROR_WALLET_NOT_FOUND, error.getMessage());
    }

    @Test
    void detail_issuer_ok() throws Exception {

        Mockito.when(walletServiceMock.getWalletDetailIssuer(INITIATIVE_ID, USER_ID))
                .thenReturn(INITIATIVE_ISSUER_DTO);

        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.get(
                                                BASE_URL + "/initiative/" + INITIATIVE_ID + "/" + USER_ID)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andReturn();

        WalletDTO walletDTO =
                objectMapper.readValue(res.getResponse().getContentAsString(), WalletDTO.class);
        assertEquals(INITIATIVE_ISSUER_DTO.getAmount(), walletDTO.getAmount());
        assertEquals(INITIATIVE_ISSUER_DTO.getAccrued(), walletDTO.getAccrued());
        assertEquals(INITIATIVE_ISSUER_DTO.getRefunded(), walletDTO.getRefunded());
    }

    @Test
    void detail_issuer_not_found() throws Exception {

        Mockito.doThrow(
                        new WalletException(
                                HttpStatus.NOT_FOUND.value(), WalletConstants.ERROR_WALLET_NOT_FOUND))
                .when(walletServiceMock)
                .getWalletDetailIssuer(INITIATIVE_ID, USER_ID);

        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.get(
                                                BASE_URL + "/initiative/" + INITIATIVE_ID + "/" + USER_ID)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(MockMvcResultMatchers.status().isNotFound())
                        .andReturn();

        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);
        assertEquals(HttpStatus.NOT_FOUND.value(), error.getCode());
        assertEquals(WalletConstants.ERROR_WALLET_NOT_FOUND, error.getMessage());
    }

    @Test
    void update_wallet_ok() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        WalletPIDTO walletPIDTO = new WalletPIDTO(INITIATIVE_ID, USER_ID, MASKED_PAN, BRAND_LOGO, BRAND_LOGO);
        List<WalletPIDTO> walletPIDTOList = new ArrayList<>();
        walletPIDTOList.add(walletPIDTO);
        WalletPIBodyDTO walletPIBodyDTO = new WalletPIBodyDTO(walletPIDTOList);

        mvc.perform(
                        MockMvcRequestBuilders.put(BASE_URL + UPDATE_WALLET_URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(walletPIBodyDTO))
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isNoContent())
                .andReturn();
    }

    @Test
    void unsubscribeInitiative_ok() throws Exception {
        mvc.perform(
                        MockMvcRequestBuilders.delete(
                                        BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + UNSUBSCRIBE_URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isNoContent())
                .andReturn();
    }

    @Test
    void unsubscribeInitiative_not_found() throws Exception {
        Mockito.doThrow(
                        new WalletException(
                                HttpStatus.NOT_FOUND.value(), WalletConstants.ERROR_WALLET_NOT_FOUND))
                .when(walletServiceMock)
                .unsubscribe(INITIATIVE_ID, USER_ID);

        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.delete(
                                                BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + UNSUBSCRIBE_URL)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(MockMvcResultMatchers.status().isNotFound())
                        .andReturn();

        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), error.getCode());
        assertEquals(WalletConstants.ERROR_WALLET_NOT_FOUND, error.getMessage());
    }

    @Test
    void processAck_ok() throws Exception {

        final InstrumentAckDTO instrumentAckDTO =
                new InstrumentAckDTO(
                        INITIATIVE_ID,
                        USER_ID,
                        WalletConstants.CHANNEL_APP_IO,
                        BRAND_LOGO,
                        BRAND_LOGO,
                        MASKED_PAN,
                        "ADD_INSTRUMENT",
                        LocalDateTime.now(),
                        1);

        Mockito.doNothing().when(walletServiceMock).processAck(instrumentAckDTO);

        mvc.perform(
                        MockMvcRequestBuilders.put(BASE_URL + PROCESS_ACK_URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(instrumentAckDTO))
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isNoContent())
                .andReturn();
    }

    @Test
    void processAck_ko_not_found() throws Exception {

        final InstrumentAckDTO instrumentAckDTO =
                new InstrumentAckDTO(
                        INITIATIVE_ID,
                        USER_ID,
                        WalletConstants.CHANNEL_APP_IO,
                        BRAND_LOGO,
                        BRAND_LOGO,
                        MASKED_PAN,
                        "ADD_INSTRUMENT",
                        LocalDateTime.now(),
                        1);

        Mockito.doThrow(
                        new WalletException(
                                HttpStatus.NOT_FOUND.value(), WalletConstants.ERROR_WALLET_NOT_FOUND))
                .when(walletServiceMock)
                .processAck(Mockito.any(InstrumentAckDTO.class));

        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.put(BASE_URL + PROCESS_ACK_URL)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .content(objectMapper.writeValueAsString(instrumentAckDTO))
                                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(MockMvcResultMatchers.status().isNotFound())
                        .andReturn();

        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), error.getCode());
        assertEquals(WalletConstants.ERROR_WALLET_NOT_FOUND, error.getMessage());
    }


    @Test
    void enroll_instrument_issuer_ok() throws Exception {

        final InstrumentIssuerDTO instrument =
                new InstrumentIssuerDTO("hpan", CHANNEL, "VISA", "VISA", "***");

        Mockito.doNothing().when(walletServiceMock).enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET);

        mvc.perform(
                        MockMvcRequestBuilders.put(
                                        BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + ENROLL_INSTRUMENT_URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(instrument))
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
    }

    @Test
    void enroll_instrument_issuer_initiative_ko() throws Exception {

        final InstrumentIssuerDTO instrument =
                new InstrumentIssuerDTO("hpan", CHANNEL, "VISA", "VISA", "***");

        Mockito.doThrow(
                        new WalletException(HttpStatus.FORBIDDEN.value(), WalletConstants.ERROR_INITIATIVE_KO))
                .when(walletServiceMock)
                .enrollInstrumentIssuer(
                        Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.any(InstrumentIssuerDTO.class));

        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.put(
                                                BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + ENROLL_INSTRUMENT_URL)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .content(objectMapper.writeValueAsString(instrument))
                                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(MockMvcResultMatchers.status().isForbidden())
                        .andReturn();

        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);

        assertEquals(HttpStatus.FORBIDDEN.value(), error.getCode());
        assertEquals(WalletConstants.ERROR_INITIATIVE_KO, error.getMessage());
    }

    @Test
    void enroll_instrument_issuer_wallet_not_found() throws Exception {

        final InstrumentIssuerDTO instrument =
                new InstrumentIssuerDTO("hpan", CHANNEL, "VISA", "VISA", "***");

        Mockito.doThrow(
                        new WalletException(
                                HttpStatus.NOT_FOUND.value(), WalletConstants.ERROR_WALLET_NOT_FOUND))
                .when(walletServiceMock)
                .enrollInstrumentIssuer(
                        Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.any(InstrumentIssuerDTO.class));

        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.put(
                                                BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + ENROLL_INSTRUMENT_URL)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .content(objectMapper.writeValueAsString(instrument))
                                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(MockMvcResultMatchers.status().isNotFound())
                        .andReturn();

        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), error.getCode());
        assertEquals(WalletConstants.ERROR_WALLET_NOT_FOUND, error.getMessage());
    }

    @Test
    void enroll_instrument_issuer_empty_body() throws Exception {

        final InstrumentIssuerDTO instrument = new InstrumentIssuerDTO("", "", "", "", "");

        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.put(
                                                BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + ENROLL_INSTRUMENT_URL)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .content(objectMapper.writeValueAsString(instrument))
                                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(MockMvcResultMatchers.status().isBadRequest())
                        .andReturn();

        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);

        assertEquals(HttpStatus.BAD_REQUEST.value(), error.getCode());
    }

    @Test
    void instrument_detail_on_initiative_ok() throws Exception {
        Mockito.when(walletServiceMock.getInitiativesWithInstrument(ID_WALLET, USER_ID))
                .thenReturn(new InitiativesWithInstrumentDTO(ID_WALLET, MASKED_PAN, BRAND, new ArrayList<>()));

        mvc.perform(
                        MockMvcRequestBuilders.get(
                                        BASE_URL + "/instrument/" + ID_WALLET + "/" + USER_ID + "/" + "initiatives")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
    }

    @Test
    void suspend_ok() throws Exception {
        Mockito.doNothing()
                .when(walletServiceMock)
                .suspendWallet(INITIATIVE_ID, USER_ID);

        mvc.perform(
                        MockMvcRequestBuilders.put(
                                        BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + SUSPEND_URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isNoContent())
                .andReturn();
    }

    @Test
    void readmit_ok() throws Exception {
        Mockito.doNothing()
                .when(walletServiceMock)
                .readmitWallet(INITIATIVE_ID, USER_ID);

        mvc.perform(
                        MockMvcRequestBuilders.put(
                                        BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + READMIT_URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isNoContent())
                .andReturn();
    }
}
