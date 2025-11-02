package it.gov.pagopa.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.wallet.config.ServiceExceptionConfig;
import it.gov.pagopa.wallet.config.WalletErrorManagerConfig;
import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.*;
import it.gov.pagopa.wallet.enums.Channel;
import it.gov.pagopa.wallet.enums.WalletStatus;
import it.gov.pagopa.wallet.exception.custom.EnrollmentNotAllowedException;
import it.gov.pagopa.wallet.exception.custom.InitiativeInvalidException;
import it.gov.pagopa.wallet.exception.custom.InvalidIbanException;
import it.gov.pagopa.wallet.exception.custom.UserNotOnboardedException;
import it.gov.pagopa.wallet.service.WalletService;
import it.gov.pagopa.wallet.utils.Utilities;
import org.iban4j.IbanFormatException;
import org.iban4j.InvalidCheckDigitException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.*;
import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionMessage.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.mock.http.server.reactive.MockServerHttpRequest.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(
        value = {WalletController.class},
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import({ServiceExceptionConfig.class, WalletErrorManagerConfig.class})
class WalletControllerTest {

    private static final String BASE_URL = "http://localhost:8080/idpay/wallet";
    private static final String USER_ID = "TEST_USER_ID";
    private static final String FAMILY_ID = "TEST_FAMILY_ID";
    private static final String ENROLL_INSTRUMENT_URL = "/instruments";
    private static final String INSTRUMENTS_URL = "/instruments/";
    private static final String ENROLL_IBAN_URL = "/iban";
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
    private static final String CHANNEL = "IO";
    private static final String IBAN_WRONG_DIGIT = "IT09P3608105138205493205496";
    private static final String IBAN_KO_NOT_IT = "GB29NWBK60161331926819";
    private static final String DESCRIPTION_OK = "conto cointestato";
    private static final String MASKED_PAN = "masked_pan";
    private static final String BRAND_LOGO = "brand_logo";
    private static final String BRAND = "brand";
    private static final String LOGO_URL = "https://test" + String.format(Utilities.LOGO_PATH_TEMPLATE,
            ORGANIZATION_ID, INITIATIVE_ID, Utilities.LOGO_NAME);
    private static final String ORGANIZATION_NAME = "TEST_ORGANIZATION_NAME";

    private static final LocalDate DATE = LocalDate.now();
    private static final LocalDateTime TEST_DATE = LocalDateTime.now();
    private static final String SERVICE_ID = "serviceid";

    private static final String USERMAIL = "USERMAIL";
    private static final String NAME = "NAME";
    private static final String SURNAME = "SURNAME";
    private static final String STATUS = "STATUS";


    private static final WalletDTO INITIATIVE_DTO_TEST =
            new WalletDTO(
                    FAMILY_ID,
                    INITIATIVE_ID,
                    INITIATIVE_ID,
                    WalletStatus.NOT_REFUNDABLE.name(),
                    null,
                    null,
                    DATE,
                    DATE,
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
                    0L,
                    List.of(),
                    SERVICE_ID,
                    USERMAIL,
                    Channel.WEB,
                    NAME,
                    SURNAME
                    );
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
                    null,
                    IBAN_OK,
                    DATE,
                    DATE,
                    DATE,
                    1,
                    45000L,
                    5000L ,
                    0L,
                    TEST_DATE,
                    WalletConstants.INITIATIVE_REWARD_TYPE_REFUND,
                    LOGO_URL,
                    ORGANIZATION_NAME,
                    10L,
                    100L,
                    0L,
                    List.of(),
                    SERVICE_ID,
                    USERMAIL,
                    Channel.WEB,
                    NAME,
                    SURNAME);
    private static final WalletDTO INITIATIVE_ISSUER_DTO =
            new WalletDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    0,
                    45000L,
                    5000L ,
                    0L,
                    TEST_DATE,
                    WalletConstants.INITIATIVE_REWARD_TYPE_REFUND,
                    LOGO_URL,
                    ORGANIZATION_NAME,
                    null,
                    100L,
                    0L,
                    List.of(),
                    SERVICE_ID,
                    USERMAIL,
                    Channel.WEB,
                    NAME,
                    SURNAME);

    private static final EvaluationDTO EVALUATION_DTO =
            new EvaluationDTO(
                    USER_ID,
                    FAMILY_ID,
                    INITIATIVE_ID,
                    INITIATIVE_ID,
                    LocalDate.now(),
                    ORGANIZATION_ID,
                    STATUS,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    null,
                    null,
                    null,
                    ORGANIZATION_NAME,
                    true,
                    null,
                    SERVICE_ID,
                    Channel.IO,
                    USERMAIL,
                    NAME,
                    SURNAME


            );


    @MockBean
    WalletService walletServiceMock;

    @Autowired
    protected MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void enroll_instrument_ok() throws Exception {

        doNothing().when(walletServiceMock).enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL);

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
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    void enroll_instrument_initiative_ko() throws Exception {
        // Given
        doThrow(new EnrollmentNotAllowedException(
                ENROLL_INSTRUMENT_DISCOUNT_INITIATIVE, PAYMENT_INSTRUMENT_ENROLL_NOT_ALLOWED_DISCOUNT_MSG))
                .when(walletServiceMock)
                .enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL);

        // When
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
                        .andExpect(status().isForbidden())
                        .andReturn();

        // Then
        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);
        assertEquals(ENROLL_INSTRUMENT_DISCOUNT_INITIATIVE, error.getCode());
        assertEquals(PAYMENT_INSTRUMENT_ENROLL_NOT_ALLOWED_DISCOUNT_MSG, error.getMessage());
    }

    @Test
    void enroll_initiative_wallet_not_found() throws Exception {
        // Given
        doThrow(new UserNotOnboardedException(String.format(USER_NOT_ONBOARDED_MSG, INITIATIVE_ID)))
                .when(walletServiceMock)
                .enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL);

        // When
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
                        .andExpect(status().isNotFound())
                        .andReturn();

        // Then
        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);
        assertEquals(USER_NOT_ONBOARDED, error.getCode());
        assertEquals(String.format(USER_NOT_ONBOARDED_MSG, INITIATIVE_ID), error.getMessage());
    }

    @Test
    void delete_instrument_ok() throws Exception {

        doNothing()
                .when(walletServiceMock)
                .deleteInstrument(INITIATIVE_ID, USER_ID, INSTRUMENT_ID, CHANNEL);

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
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    void delete_instrument_initiative_ko() throws Exception {
        // Given
        doThrow(new InitiativeInvalidException(String.format(INITIATIVE_ENDED_MSG, INITIATIVE_ID)))
                .when(walletServiceMock)
                .deleteInstrument(INITIATIVE_ID, USER_ID, INSTRUMENT_ID, CHANNEL);

        // When
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
                        .andExpect(status().isForbidden())
                        .andReturn();

        // Then
        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);
        assertEquals(INITIATIVE_ENDED, error.getCode());
        assertEquals(String.format(INITIATIVE_ENDED_MSG, INITIATIVE_ID), error.getMessage());
    }

    @Test
    void delete_instrument_initiative_wallet_not_found() throws Exception {
        // Given
        doThrow(new UserNotOnboardedException(String.format(USER_NOT_ONBOARDED_MSG, INITIATIVE_ID)))
                .when(walletServiceMock)
                .deleteInstrument(INITIATIVE_ID, USER_ID, INSTRUMENT_ID, CHANNEL);

        // When
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
                        .andExpect(status().isNotFound())
                        .andReturn();

        // Then
        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);
        assertEquals(USER_NOT_ONBOARDED, error.getCode());
        assertEquals(String.format(USER_NOT_ONBOARDED_MSG, INITIATIVE_ID), error.getMessage());
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
                        .andExpect(status().isOk())
                        .andReturn();

        EnrollmentStatusDTO statusDTO =
                objectMapper.readValue(res.getResponse().getContentAsString(), EnrollmentStatusDTO.class);
        assertEquals(WalletStatus.NOT_REFUNDABLE.name(), statusDTO.getStatus());
    }

    @Test
    void status_not_found() throws Exception {
        // Given
        doThrow(new UserNotOnboardedException(String.format(USER_NOT_ONBOARDED_MSG, INITIATIVE_ID)))
                .when(walletServiceMock)
                .getEnrollmentStatus(INITIATIVE_ID, USER_ID);

        // When
        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.get(
                                                BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + STATUS_URL)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(status().isNotFound())
                        .andReturn();

        // Then
        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);
        assertEquals(USER_NOT_ONBOARDED, error.getCode());
        assertEquals(String.format(USER_NOT_ONBOARDED_MSG, INITIATIVE_ID), error.getMessage());
    }

    @Test
    void enroll_iban_wallet_not_found() throws Exception {
        // Given
        doThrow(new UserNotOnboardedException(String.format(USER_NOT_ONBOARDED_MSG, INITIATIVE_ID)))
                .when(walletServiceMock)
                .enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, CHANNEL, DESCRIPTION_OK);

        // When
        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.put(
                                                BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + ENROLL_IBAN_URL)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .content(objectMapper.writeValueAsString(IBAN_BODY_DTO))
                                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(status().isNotFound())
                        .andReturn();

        // Then
        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);
        assertEquals(USER_NOT_ONBOARDED, error.getCode());
        assertEquals(String.format(USER_NOT_ONBOARDED_MSG, INITIATIVE_ID), error.getMessage());
    }

    @Test
    void enroll_iban_wallet_format() throws Exception {
        // Given
        final IbanBodyDTO iban = new IbanBodyDTO(IBAN_WRONG, DESCRIPTION_OK, CHANNEL);

        doThrow(new IbanFormatException())
                .when(walletServiceMock)
                .enrollIban(INITIATIVE_ID, USER_ID, IBAN_WRONG, CHANNEL, DESCRIPTION_OK);

        // When
        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.put(
                                                BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + ENROLL_IBAN_URL)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .content(objectMapper.writeValueAsString(iban))
                                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(status().isBadRequest())
                        .andReturn();

        // Then
        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);
        assertEquals(INVALID_REQUEST, error.getCode());
    }

    @Test
    void enroll_iban_invalid_digit() throws Exception {
        // Given
        final IbanBodyDTO iban = new IbanBodyDTO(IBAN_KO_NOT_IT, DESCRIPTION_OK, CHANNEL);

        doThrow(new InvalidCheckDigitException())
                .when(walletServiceMock)
                .enrollIban(INITIATIVE_ID, USER_ID, IBAN_KO_NOT_IT, CHANNEL, DESCRIPTION_OK);

        // When
        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.put(
                                                BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + ENROLL_IBAN_URL)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .content(objectMapper.writeValueAsString(iban))
                                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(status().isBadRequest())
                        .andReturn();

        // Then
        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);
        assertEquals(INVALID_REQUEST, error.getCode());
    }

    @Test
    void enroll_iban_not_it() throws Exception {
        // Given
        final IbanBodyDTO iban = new IbanBodyDTO(IBAN_WRONG_DIGIT, DESCRIPTION_OK, CHANNEL);

        doThrow(new InvalidIbanException(String.format(ERROR_IBAN_NOT_ITALIAN, IBAN_WRONG_DIGIT)))
                .when(walletServiceMock)
                .enrollIban(INITIATIVE_ID, USER_ID, IBAN_WRONG_DIGIT, CHANNEL, DESCRIPTION_OK);

        // When
        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.put(
                                                BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + ENROLL_IBAN_URL)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .content(objectMapper.writeValueAsString(iban))
                                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(status().isForbidden())
                        .andReturn();

        // Then
        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);
        assertEquals(IBAN_NOT_ITALIAN, error.getCode());
        assertEquals(String.format(ERROR_IBAN_NOT_ITALIAN, IBAN_WRONG_DIGIT), error.getMessage());
    }

    @Test
    void enroll_iban_empty_body() throws Exception {
        // When
        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.put(
                                                BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + ENROLL_IBAN_URL)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .content(objectMapper.writeValueAsString(IBAN_BODY_DTO_EMPTY))
                                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(status().isBadRequest())
                        .andReturn();

        // Then
        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);
        assertEquals(INVALID_REQUEST, error.getCode());
        assertTrue(error.getMessage().contains(WalletConstants.ERROR_MANDATORY_FIELD));
    }

    @Test
    void enroll_iban_initiative_ko() throws Exception {
        // Given
        doThrow(new InitiativeInvalidException(String.format(INITIATIVE_ENDED_MSG, INITIATIVE_ID)))
                .when(walletServiceMock)
                .enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, CHANNEL, DESCRIPTION_OK);

        // When
        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.put(
                                                BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + ENROLL_IBAN_URL)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .content(objectMapper.writeValueAsString(IBAN_BODY_DTO))
                                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(status().isForbidden())
                        .andReturn();

        // Then
        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);
        assertEquals(INITIATIVE_ENDED, error.getCode());
        assertEquals(String.format(INITIATIVE_ENDED_MSG, INITIATIVE_ID), error.getMessage());
    }

    @Test
    void enroll_iban_ok() throws Exception {
        doNothing()
                .when(walletServiceMock)
                .enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, CHANNEL, DESCRIPTION_OK);

        mvc.perform(
                        MockMvcRequestBuilders.put(
                                        BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + ENROLL_IBAN_URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(IBAN_BODY_DTO))
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
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
                .andExpect(status().isOk())
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
                        .andExpect(status().isOk())
                        .andReturn();

        WalletDTO walletDTO =
                objectMapper.readValue(res.getResponse().getContentAsString(), WalletDTO.class);
        assertEquals(INITIATIVE_DTO.getInitiativeId(), walletDTO.getInitiativeId());
        assertEquals(INITIATIVE_DTO.getInitiativeName(), walletDTO.getInitiativeName());
        assertEquals(INITIATIVE_DTO.getStatus(), walletDTO.getStatus());
        assertEquals(INITIATIVE_DTO.getInitiativeEndDate(), walletDTO.getInitiativeEndDate());
        assertEquals(INITIATIVE_DTO.getIban(), walletDTO.getIban());
        assertEquals(INITIATIVE_DTO.getNInstr(), walletDTO.getNInstr());
        assertEquals(INITIATIVE_DTO.getAmountCents(), walletDTO.getAmountCents());
        assertEquals(INITIATIVE_DTO.getAccruedCents(), walletDTO.getAccruedCents());
        assertEquals(INITIATIVE_DTO.getRefundedCents(), walletDTO.getRefundedCents());
        assertEquals(INITIATIVE_DTO.getNTrx(), walletDTO.getNTrx());
        assertEquals(INITIATIVE_DTO.getMaxTrx(),walletDTO.getMaxTrx());
    }

    @Test
    void detail_not_found() throws Exception {
        // Given
        doThrow(new UserNotOnboardedException(String.format(USER_NOT_ONBOARDED_MSG, INITIATIVE_ID)))
                .when(walletServiceMock)
                .getWalletDetail(INITIATIVE_ID, USER_ID);

        // When
        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.get(BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(status().isNotFound())
                        .andReturn();

        // Then
        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);
        assertEquals(USER_NOT_ONBOARDED, error.getCode());
        assertEquals(String.format(USER_NOT_ONBOARDED_MSG, INITIATIVE_ID), error.getMessage());
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
                        .andExpect(status().isOk())
                        .andReturn();

        WalletDTO walletDTO =
                objectMapper.readValue(res.getResponse().getContentAsString(), WalletDTO.class);
        assertEquals(INITIATIVE_ISSUER_DTO.getAmountCents(), walletDTO.getAmountCents());
        assertEquals(INITIATIVE_ISSUER_DTO.getAccruedCents(), walletDTO.getAccruedCents());
        assertEquals(INITIATIVE_ISSUER_DTO.getRefundedCents(), walletDTO.getRefundedCents());
    }

    @Test
    void detail_issuer_not_found() throws Exception {
        // Given
        doThrow(new UserNotOnboardedException(String.format(USER_NOT_ONBOARDED_MSG, INITIATIVE_ID)))
                .when(walletServiceMock)
                .getWalletDetailIssuer(INITIATIVE_ID, USER_ID);

        // When
        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.get(
                                                BASE_URL + "/initiative/" + INITIATIVE_ID + "/" + USER_ID)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(status().isNotFound())
                        .andReturn();

        // Then
        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);
        assertEquals(USER_NOT_ONBOARDED, error.getCode());
        assertEquals(String.format(USER_NOT_ONBOARDED_MSG, INITIATIVE_ID), error.getMessage());
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
                .andExpect(status().isNoContent())
                .andReturn();
    }

    @Test
    void unsubscribeInitiative_ok() throws Exception {
        mvc.perform(
                        MockMvcRequestBuilders.delete(
                                        BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + UNSUBSCRIBE_URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent())
                .andReturn();
    }

    @Test
    void unsubscribeInitiative_not_found() throws Exception {
        // Given
        doThrow(new UserNotOnboardedException(String.format(USER_NOT_ONBOARDED_MSG, INITIATIVE_ID)))
                .when(walletServiceMock)
                .unsubscribe(INITIATIVE_ID, USER_ID, CHANNEL);

        // When
        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.delete(
                                                BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + UNSUBSCRIBE_URL)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(status().isNotFound())
                        .andReturn();

        // Then
        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);
        assertEquals(USER_NOT_ONBOARDED, error.getCode());
        assertEquals(String.format(USER_NOT_ONBOARDED_MSG, INITIATIVE_ID), error.getMessage());
    }

    @Test
    void processAck_ok() throws Exception {

        final InstrumentAckDTO instrumentAckDTO =
                new InstrumentAckDTO(
                        INITIATIVE_ID,
                        USER_ID,
                        WalletConstants.CHANNEL_APP_IO,
                        WalletConstants.INSTRUMENT_TYPE_CARD,
                        BRAND_LOGO,
                        BRAND_LOGO,
                        MASKED_PAN,
                        "ADD_INSTRUMENT",
                        LocalDateTime.now(),
                        1);

        doNothing().when(walletServiceMock).processAck(instrumentAckDTO);

        mvc.perform(
                        MockMvcRequestBuilders.put(BASE_URL + PROCESS_ACK_URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(instrumentAckDTO))
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent())
                .andReturn();
    }

    @Test
    void processAck_ko_not_found() throws Exception {
        // Given
        final InstrumentAckDTO instrumentAckDTO =
                new InstrumentAckDTO(
                        INITIATIVE_ID,
                        USER_ID,
                        WalletConstants.CHANNEL_APP_IO,
                        WalletConstants.INSTRUMENT_TYPE_CARD,
                        BRAND_LOGO,
                        BRAND_LOGO,
                        MASKED_PAN,
                        "ADD_INSTRUMENT",
                        LocalDateTime.now(),
                        1);

        doThrow(new UserNotOnboardedException(String.format(USER_NOT_ONBOARDED_MSG, INITIATIVE_ID)))
                .when(walletServiceMock)
                .processAck(any(InstrumentAckDTO.class));

        // When
        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.put(BASE_URL + PROCESS_ACK_URL)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .content(objectMapper.writeValueAsString(instrumentAckDTO))
                                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(status().isNotFound())
                        .andReturn();

        // Then
        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);
        assertEquals(USER_NOT_ONBOARDED, error.getCode());
        assertEquals(String.format(USER_NOT_ONBOARDED_MSG, INITIATIVE_ID), error.getMessage());
    }


    @Test
    void enroll_instrument_issuer_ok() throws Exception {

        final InstrumentIssuerDTO instrument =
                new InstrumentIssuerDTO("hpan", CHANNEL, "VISA", "VISA", "***");

        doNothing().when(walletServiceMock).enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL);

        mvc.perform(
                        MockMvcRequestBuilders.put(
                                        BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + ENROLL_INSTRUMENT_URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(instrument))
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    void enroll_instrument_issuer_initiative_ko() throws Exception {
        // Given
        final InstrumentIssuerDTO instrument =
                new InstrumentIssuerDTO("hpan", CHANNEL, "VISA", "VISA", "***");

        doThrow(new InitiativeInvalidException(String.format(INITIATIVE_ENDED_MSG, INITIATIVE_ID)))
                .when(walletServiceMock)
                .enrollInstrumentIssuer(
                        Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), any(InstrumentIssuerDTO.class));

        // When
        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.put(
                                                BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + ENROLL_INSTRUMENT_URL)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .content(objectMapper.writeValueAsString(instrument))
                                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(status().isForbidden())
                        .andReturn();

        // Then
        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);
        assertEquals(INITIATIVE_ENDED, error.getCode());
        assertEquals(String.format(INITIATIVE_ENDED_MSG, INITIATIVE_ID), error.getMessage());
    }

    @Test
    void enroll_instrument_issuer_wallet_not_found() throws Exception {
        // Given
        final InstrumentIssuerDTO instrument =
                new InstrumentIssuerDTO("hpan", CHANNEL, "VISA", "VISA", "***");

        doThrow(new UserNotOnboardedException(String.format(USER_NOT_ONBOARDED_MSG, INITIATIVE_ID)))
                .when(walletServiceMock)
                .enrollInstrumentIssuer(
                        Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), any(InstrumentIssuerDTO.class));

        // When
        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.put(
                                                BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + ENROLL_INSTRUMENT_URL)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .content(objectMapper.writeValueAsString(instrument))
                                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(status().isNotFound())
                        .andReturn();

        // Then
        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);
        assertEquals(USER_NOT_ONBOARDED, error.getCode());
        assertEquals(String.format(USER_NOT_ONBOARDED_MSG, INITIATIVE_ID), error.getMessage());
    }

    @Test
    void enroll_instrument_issuer_empty_body() throws Exception {
        // Given
        final InstrumentIssuerDTO instrument = new InstrumentIssuerDTO("", "", "", "", "");

        // When
        MvcResult res =
                mvc.perform(
                                MockMvcRequestBuilders.put(
                                                BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + ENROLL_INSTRUMENT_URL)
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .content(objectMapper.writeValueAsString(instrument))
                                        .accept(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(status().isBadRequest())
                        .andReturn();

        // Then
        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);
        assertEquals(INVALID_REQUEST, error.getCode());
        assertTrue(error.getMessage().contains(WalletConstants.ERROR_MANDATORY_FIELD));
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
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    void suspend_ok() throws Exception {
        doNothing()
                .when(walletServiceMock)
                .suspendWallet(INITIATIVE_ID, USER_ID);

        mvc.perform(
                        MockMvcRequestBuilders.put(
                                        BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + SUSPEND_URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent())
                .andReturn();
    }

    @Test
    void readmit_ok() throws Exception {
        doNothing()
                .when(walletServiceMock)
                .readmitWallet(INITIATIVE_ID, USER_ID);

        mvc.perform(
                        MockMvcRequestBuilders.put(
                                        BASE_URL + "/" + INITIATIVE_ID + "/" + USER_ID + READMIT_URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent())
                .andReturn();
    }

    @Test
    void enroll_instrument_code_ok() throws Exception {
        doNothing().when(walletServiceMock).enrollInstrumentCode(INITIATIVE_ID, USER_ID, CHANNEL);

        mvc.perform(
                        MockMvcRequestBuilders.put(
                                        BASE_URL
                                                + "/"
                                                + INITIATIVE_ID
                                                + "/"
                                                + USER_ID
                                                + "/code"
                                                + ENROLL_INSTRUMENT_URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    void enroll_instrument_code_ko() throws Exception {
        // Given
        doThrow(new InitiativeInvalidException(String.format(INITIATIVE_ENDED_MSG, INITIATIVE_ID)))
                .when(walletServiceMock)
                .enrollInstrumentCode(INITIATIVE_ID, USER_ID, CHANNEL);

        // When
        MvcResult res = mvc.perform(
                        MockMvcRequestBuilders.put(
                                        BASE_URL
                                                + "/"
                                                + INITIATIVE_ID
                                                + "/"
                                                + USER_ID
                                                + "/code"
                                                + ENROLL_INSTRUMENT_URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isForbidden())
                .andReturn();

        // Then
        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);
        assertEquals(INITIATIVE_ENDED, error.getCode());
        assertEquals(String.format(INITIATIVE_ENDED_MSG, INITIATIVE_ID), error.getMessage());
    }

    @Test
    void enroll_instrument_code_ko_genericServiceException() throws Exception {
        // Given
        doThrow(new ServiceException("DUMMY_EXCEPTION_CODE", "DUMMY_EXCEPTION_MESSAGE"))
                .when(walletServiceMock)
                .enrollInstrumentCode(INITIATIVE_ID, USER_ID, CHANNEL);

        // When
        MvcResult res = mvc.perform(
                        MockMvcRequestBuilders.put(
                                        BASE_URL
                                                + "/"
                                                + INITIATIVE_ID
                                                + "/"
                                                + USER_ID
                                                + "/code"
                                                + ENROLL_INSTRUMENT_URL)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isInternalServerError())
                .andReturn();

        // Then
        ErrorDTO error = objectMapper.readValue(res.getResponse().getContentAsString(), ErrorDTO.class);
        assertEquals("DUMMY_EXCEPTION_CODE", error.getCode());
        assertEquals("DUMMY_EXCEPTION_MESSAGE", error.getMessage());
    }

    @Test
    void createWallet_Success() throws Exception {
    doNothing().when(walletServiceMock).createWallet(any(EvaluationDTO.class));

        mvc.perform(MockMvcRequestBuilders.put(
                        BASE_URL
                                + "/createWallet")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(EVALUATION_DTO))
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
    }
}

