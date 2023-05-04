package it.gov.pagopa.wallet.service;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import it.gov.pagopa.wallet.connector.InitiativeRestConnector;
import it.gov.pagopa.wallet.connector.OnboardingRestConnector;
import it.gov.pagopa.wallet.connector.PaymentInstrumentRestConnector;
import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.*;
import it.gov.pagopa.wallet.dto.mapper.TimelineMapper;
import it.gov.pagopa.wallet.dto.mapper.WalletMapper;
import it.gov.pagopa.wallet.enums.WalletStatus;
import it.gov.pagopa.wallet.event.producer.ErrorProducer;
import it.gov.pagopa.wallet.event.producer.IbanProducer;
import it.gov.pagopa.wallet.event.producer.NotificationProducer;
import it.gov.pagopa.wallet.event.producer.TimelineProducer;
import it.gov.pagopa.wallet.exception.WalletException;
import it.gov.pagopa.wallet.model.Wallet;
import it.gov.pagopa.wallet.model.Wallet.RefundHistory;
import it.gov.pagopa.wallet.repository.WalletRepository;
import it.gov.pagopa.wallet.repository.WalletUpdatesRepository;
import it.gov.pagopa.wallet.utils.AuditUtilities;
import it.gov.pagopa.wallet.utils.Utilities;
import org.iban4j.IbanFormatException;
import org.iban4j.InvalidCheckDigitException;
import org.iban4j.UnsupportedCountryException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static it.gov.pagopa.wallet.constants.WalletConstants.STATUS_KO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = WalletServiceImpl.class)
class WalletServiceTest {

    @MockBean
    IbanProducer ibanProducer;
    @MockBean
    TimelineProducer timelineProducer;
    @MockBean
    ErrorProducer errorProducer;
    @MockBean
    NotificationProducer notificationProducer;
    @MockBean
    WalletRepository walletRepositoryMock;
    @MockBean
    WalletUpdatesRepository walletUpdatesRepositoryMock;
    @MockBean
    PaymentInstrumentRestConnector paymentInstrumentRestConnector;
    @MockBean
    OnboardingRestConnector onboardingRestConnector;
    @MockBean
    InitiativeRestConnector initiativeRestConnector;
    @MockBean
    WalletMapper walletMapper;
    @MockBean
    TimelineMapper timelineMapper;
    @Autowired
    WalletService walletService;
    @MockBean
    AuditUtilities auditUtilities;
    @MockBean
    Utilities utilities;

    private static final String USER_ID = "TEST_USER_ID";
    private static final String FAMILY_ID = "TEST_FAMILY_ID";
    private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";
    private static final String INITIATIVE_ID_REFUNDABLE = "TEST_INITIATIVE_ID_REFUNDABLE";
    private static final String INITIATIVE_ID_UNSUBSCRIBED = "TEST_INITIATIVE_ID_UNSUBSCRIBED";
    private static final String INITIATIVE_NAME = "TEST_INITIATIVE_NAME";
    private static final String ORGANIZATION_ID = "TEST_ORGANIZATION_ID";
    private static final String ORGANIZATION_NAME = "TEST_ORGANIZATION_NAME";
    private static final String MASKED_PAN = "masked_pan";
    private static final String BRAND_LOGO = "brand_logo";
    private static final String BRAND = "brand";
    private static final String CHANNEL = "CHANNEL";
    private static final String ID_WALLET = "TEST_ID_WALLET";
    private static final String INSTRUMENT_ID = "TEST_INSTRUMENT_ID";
    private static final String IBAN_OK = "IT09P3608105138205493205495";
    private static final String IBAN_OK_OTHER = "IT09P3608105138205493205494";
    private static final String IBAN_KO_NOT_IT = "GB29NWBK60161331926819";
    private static final String IBAN_WRONG = "it99C1234567890123456789012222";
    private static final String IBAN_WRONG_DIGIT = "IT09P3608105138205493205496";
    private static final String DESCRIPTION_OK = "conto cointestato";
    private static final LocalDateTime TEST_DATE = LocalDateTime.now();
    private static final LocalDateTime TEST_SUSPENSION_DATE = LocalDateTime.now().minusDays(1);
    private static final LocalDate TEST_DATE_ONLY_DATE = LocalDate.now();
    private static final LocalDate TEST_END_DATE = LocalDate.now().plusDays(2);
    private static final BigDecimal TEST_AMOUNT = BigDecimal.valueOf(2.00);
    private static final BigDecimal TEST_ACCRUED = BigDecimal.valueOf(40.00);
    private static final BigDecimal TEST_REFUNDED = BigDecimal.valueOf(0.00);
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PENDING_ENROLLMENT = "PENDING_ENROLLMENT_REQUEST";
    private static final String WALLET_NOT_REFUNDABLE = "NOT_REFUNDABLE";
    private static final String WALLET_REFUNDABLE = "NOT_REFUNDABLE";
    private static final String WALLET_NOT_REFUNDABLE_ONLY_INSTRUMENT = "NOT_REFUNDABLE_ONLY_INSTRUMENT";
    private static final String WALLET_NOT_REFUNDABLE_ONLY_IBAN = "NOT_REFUNDABLE_ONLY_IBAN";
    private static final LocalDate START_DATE = LocalDate.now();
    private static final LocalDate END_DATE = LocalDate.now().plusDays(2);
    private static final LocalDate TRANSFER_DATE = LocalDate.now();
    private static final LocalDate NOTIFICATION_DATE = LocalDate.now();
    private static final String REWARD_STATUS = "reward_status";
    private static final String REFUND_TYPE = "refund_type";
    private static final String LOGO_URL = "https://test" + String.format(Utilities.LOGO_PATH_TEMPLATE,
            ORGANIZATION_ID, INITIATIVE_ID, Utilities.LOGO_NAME);

    private static final Wallet TEST_WALLET =
            Wallet.builder()
                    .userId(USER_ID)
                    .initiativeId(INITIATIVE_ID)
                    .initiativeName(INITIATIVE_NAME)
                    .acceptanceDate(TEST_DATE)
                    .status(WalletStatus.NOT_REFUNDABLE.name())
                    .endDate(TEST_DATE_ONLY_DATE)
                    .amount(TEST_AMOUNT)
                    .accrued(TEST_ACCRUED)
                    .refunded(TEST_REFUNDED)
                    .lastCounterUpdate(TEST_DATE)
                    .build();
    private static final Wallet TEST_WALLET_DISCOUNT =
            Wallet.builder()
                    .userId(USER_ID)
                    .initiativeId(INITIATIVE_ID)
                    .initiativeName(INITIATIVE_NAME)
                    .acceptanceDate(TEST_DATE)
                    .status(WalletStatus.NOT_REFUNDABLE.name())
                    .endDate(TEST_DATE_ONLY_DATE)
                    .amount(TEST_AMOUNT)
                    .accrued(TEST_ACCRUED)
                    .refunded(TEST_REFUNDED)
                    .lastCounterUpdate(TEST_DATE)
                    .initiativeRewardType(WalletConstants.INITIATIVE_REWARD_TYPE_DISCOUNT)
                    .build();
    private static final Wallet TEST_WALLET_REFUNDABLE =
            Wallet.builder()
                    .userId(USER_ID)
                    .initiativeId(INITIATIVE_ID_REFUNDABLE)
                    .initiativeName(INITIATIVE_NAME)
                    .acceptanceDate(TEST_DATE)
                    .status(WalletStatus.REFUNDABLE.name())
                    .endDate(TEST_DATE_ONLY_DATE)
                    .amount(TEST_AMOUNT)
                    .accrued(TEST_ACCRUED)
                    .refunded(TEST_REFUNDED)
                    .lastCounterUpdate(TEST_DATE)
                    .build();
    private static final Wallet TEST_WALLET_UNSUBSCRIBED =
            Wallet.builder()
                    .userId(USER_ID)
                    .initiativeId(INITIATIVE_ID_UNSUBSCRIBED)
                    .initiativeName(INITIATIVE_NAME)
                    .acceptanceDate(TEST_DATE)
                    .status(WalletStatus.UNSUBSCRIBED)
                    .endDate(TEST_DATE_ONLY_DATE)
                    .amount(TEST_AMOUNT)
                    .accrued(TEST_ACCRUED)
                    .refunded(TEST_REFUNDED)
                    .build();

    private static final Wallet TEST_WALLET_SUSPENDED =
            Wallet.builder()
                    .userId(USER_ID)
                    .initiativeId(INITIATIVE_ID)
                    .initiativeName(INITIATIVE_NAME)
                    .acceptanceDate(TEST_DATE)
                    .status(WalletStatus.SUSPENDED)
                    .endDate(TEST_DATE_ONLY_DATE)
                    .amount(TEST_AMOUNT)
                    .accrued(TEST_ACCRUED)
                    .refunded(TEST_REFUNDED)
                    .lastCounterUpdate(TEST_DATE)
                    .updateDate(TEST_DATE)
                    .suspensionDate(TEST_SUSPENSION_DATE)
                    .build();

    private static final Wallet TEST_WALLET_ISSUER =
            Wallet.builder().amount(TEST_AMOUNT).accrued(TEST_ACCRUED).refunded(TEST_REFUNDED).build();

    private static final QueueOperationDTO TEST_OPERATION_DTO =
            QueueOperationDTO.builder().userId(USER_ID).build();

    private static final Wallet TEST_WALLET_2 =
            Wallet.builder()
                    .userId(USER_ID)
                    .initiativeId(INITIATIVE_ID)
                    .initiativeName(INITIATIVE_NAME)
                    .acceptanceDate(TEST_DATE)
                    .status(WalletStatus.NOT_REFUNDABLE.name())
                    .endDate(TEST_DATE_ONLY_DATE)
                    .amount(TEST_AMOUNT)
                    .accrued(TEST_ACCRUED)
                    .refunded(TEST_REFUNDED)
                    .lastCounterUpdate(TEST_DATE)
                    .build();

    private static final WalletDTO WALLET_DTO =
            new WalletDTO(
                    FAMILY_ID,
                    INITIATIVE_ID,
                    INITIATIVE_NAME,
                    WalletStatus.NOT_REFUNDABLE.name(),
                    IBAN_OK,
                    TEST_DATE_ONLY_DATE,
                    0,
                    TEST_AMOUNT,
                    TEST_ACCRUED,
                    TEST_REFUNDED,
                    TEST_DATE,
                    WalletConstants.INITIATIVE_REWARD_TYPE_REFUND,
                    LOGO_URL,
                    ORGANIZATION_NAME);

    private static final WalletDTO WALLET_REFUNDABLE_DTO =
            new WalletDTO(
                    FAMILY_ID,
                    INITIATIVE_ID_REFUNDABLE,
                    INITIATIVE_NAME,
                    WalletStatus.REFUNDABLE.name(),
                    IBAN_OK,
                    TEST_END_DATE,
                    0,
                    TEST_AMOUNT,
                    TEST_ACCRUED,
                    TEST_REFUNDED,
                    TEST_DATE,
                    WalletConstants.INITIATIVE_REWARD_TYPE_REFUND,
                    LOGO_URL,
                    ORGANIZATION_NAME);

    private static final WalletDTO WALLET_UNSUBSCRIBED_DTO =
            new WalletDTO(
                    FAMILY_ID,
                    INITIATIVE_ID_UNSUBSCRIBED,
                    INITIATIVE_NAME,
                    WalletStatus.UNSUBSCRIBED,
                    IBAN_OK,
                    TEST_END_DATE,
                    0,
                    TEST_AMOUNT,
                    TEST_ACCRUED,
                    TEST_REFUNDED,
                    TEST_DATE,
                    WalletConstants.INITIATIVE_REWARD_TYPE_REFUND,
                    LOGO_URL,
                    ORGANIZATION_NAME);


    private static final WalletDTO WALLET_ISSUER_DTO =
            new WalletDTO(null, null, null, null, null, null, 0, TEST_AMOUNT, TEST_ACCRUED, TEST_REFUNDED, TEST_DATE,
                    WalletConstants.INITIATIVE_REWARD_TYPE_REFUND, LOGO_URL, ORGANIZATION_NAME);

    private static final RewardDTO REWARD_DTO =
            RewardDTO.builder()
                    .counters(new Counters(false, 1L, TEST_AMOUNT, TEST_AMOUNT, TEST_ACCRUED))
                    .build();

    private static final RewardTransactionDTO REWARD_TRX_DTO_REWARDED =
            RewardTransactionDTO.builder()
                    .userId(USER_ID)
                    .status("REWARDED")
                    .rewards(Map.of(INITIATIVE_ID, REWARD_DTO))
                    .build();

    private static final RewardTransactionDTO REWARD_TRX_DTO =
            RewardTransactionDTO.builder()
                    .status("NOT_REWARDED")
                    .rewards(Map.of(INITIATIVE_ID, new RewardDTO()))
                    .build();
    private static final EvaluationDTO OUTCOME_KO =
            new EvaluationDTO(
                    USER_ID,
                    FAMILY_ID,
                    INITIATIVE_ID,
                    INITIATIVE_ID,
                    TEST_DATE_ONLY_DATE,
                    INITIATIVE_ID,
                    WalletConstants.STATUS_ONBOARDING_KO,
                    TEST_DATE,
                    TEST_DATE,
                    List.of(),
                    new BigDecimal(500),
                    WalletConstants.INITIATIVE_REWARD_TYPE_REFUND,
                    ORGANIZATION_NAME,
                    Boolean.FALSE);

    private static final EvaluationDTO EVALUATION_ONBOARDING_OK =
            new EvaluationDTO(
                    USER_ID,
                    FAMILY_ID,
                    INITIATIVE_ID,
                    INITIATIVE_ID,
                    TEST_DATE_ONLY_DATE,
                    INITIATIVE_ID,
                    WalletConstants.STATUS_ONBOARDING_OK,
                    TEST_DATE,
                    TEST_DATE,
                    List.of(),
                    new BigDecimal(500),
                    WalletConstants.INITIATIVE_REWARD_TYPE_REFUND,
                    ORGANIZATION_NAME,
                    Boolean.FALSE);

    private static final EvaluationDTO EVALUATION_JOINED =
            new EvaluationDTO(
                    USER_ID,
                    FAMILY_ID,
                    INITIATIVE_ID,
                    INITIATIVE_ID,
                    TEST_DATE_ONLY_DATE,
                    INITIATIVE_ID,
                    WalletConstants.STATUS_JOINED,
                    TEST_DATE,
                    TEST_DATE,
                    List.of(),
                    new BigDecimal(500),
                    WalletConstants.INITIATIVE_REWARD_TYPE_REFUND,
                    ORGANIZATION_NAME,
                    Boolean.FALSE);

    private static final EvaluationDTO OUTCOME_OK_DISCOUNT =
            new EvaluationDTO(
                    USER_ID,
                    FAMILY_ID,
                    INITIATIVE_ID,
                    INITIATIVE_ID,
                    TEST_DATE_ONLY_DATE,
                    INITIATIVE_ID,
                    WalletConstants.STATUS_ONBOARDING_OK,
                    TEST_DATE,
                    TEST_DATE,
                    List.of(),
                    new BigDecimal(500),
                    WalletConstants.INITIATIVE_REWARD_TYPE_DISCOUNT,
                    ORGANIZATION_NAME,
                    Boolean.FALSE);

    @Test
    void enrollInstrument_ok() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        TEST_WALLET.setIban(null);
        TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE.name());
        TEST_WALLET.setNInstr(0);
        TEST_WALLET.setEndDate(LocalDate.MAX);

        Mockito.doNothing()
                .when(paymentInstrumentRestConnector)
                .enrollInstrument(Mockito.any(InstrumentCallBodyDTO.class));

        try {
            walletService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET);
        } catch (WalletException e) {
            Assertions.fail();
        }
        assertEquals(WalletStatus.NOT_REFUNDABLE.name(), TEST_WALLET.getStatus());
        assertEquals(0, TEST_WALLET.getNInstr());
    }

    @Test
    void enrollInstrument_ko_feignexception() {
        TEST_WALLET.setEndDate(LocalDate.MAX);
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        Request request =
                Request.create(Request.HttpMethod.PUT, "url", new HashMap<>(), null, new RequestTemplate());

        Mockito.doThrow(new FeignException.BadRequest("", request, new byte[0], null))
                .when(paymentInstrumentRestConnector)
                .enrollInstrument(Mockito.any(InstrumentCallBodyDTO.class));

        try {
            walletService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET);
            Assertions.fail();
        } catch (WalletException e) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), e.getCode());
        }
    }

    @Test
    void enrollInstrument_ko_discountInitiative() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET_DISCOUNT));

        try {
            walletService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET);
        } catch (WalletException e) {
            assertEquals(HttpStatus.FORBIDDEN.value(), e.getCode());
            assertEquals(WalletConstants.ERROR_INITIATIVE_DISCOUNT_PI, e.getMessage());
        }

    }

    @Test
    void processAck() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE.name());
        TEST_WALLET.setNInstr(0);
        TEST_WALLET.setIban(null);

        Mockito.doNothing().when(timelineProducer).sendEvent(Mockito.any(QueueOperationDTO.class));
        Mockito.doNothing().when(errorProducer).sendEvent(Mockito.any());
        final InstrumentAckDTO instrumentAckDTO =
                new InstrumentAckDTO(
                        INITIATIVE_ID,
                        USER_ID,
                        WalletConstants.CHANNEL_APP_IO,
                        BRAND_LOGO,
                        BRAND_LOGO,
                        MASKED_PAN,
                        "ADD_INSTRUMENT",
                        TEST_DATE,
                        1);
        Mockito.when(timelineMapper.ackToTimeline(instrumentAckDTO)).thenReturn(TEST_OPERATION_DTO);


        try {
            walletService.processAck(instrumentAckDTO);
        } catch (WalletException e) {
            Assertions.fail();
        }
        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(1))
                .updateInstrumentNumber(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyInt(), Mockito.anyString());
        Mockito.verify(errorProducer, Mockito.times(0)).sendEvent(Mockito.any());
    }

    @Test
    void processAck_not_found() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.empty());

        TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE.name());
        TEST_WALLET.setNInstr(0);
        TEST_WALLET.setIban(null);

        final InstrumentAckDTO instrumentAckDTO =
                new InstrumentAckDTO(
                        INITIATIVE_ID,
                        USER_ID,
                        WalletConstants.CHANNEL_APP_IO,
                        BRAND_LOGO,
                        BRAND_LOGO,
                        MASKED_PAN,
                        "ADD_INSTRUMENT",
                        TEST_DATE,
                        1);
        Mockito.when(timelineMapper.ackToTimeline(instrumentAckDTO)).thenReturn(TEST_OPERATION_DTO);

        try {
            walletService.processAck(instrumentAckDTO);
        } catch (WalletException e) {
            assertEquals(HttpStatus.NOT_FOUND.value(), e.getCode());
        }
    }

    @Test
    void processAck_ko() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(Mockito.anyString(), Mockito.anyString())).thenReturn(Optional.of(TEST_WALLET));
        Mockito.doNothing().when(timelineProducer).sendEvent(Mockito.any(QueueOperationDTO.class));
        Mockito.doNothing().when(errorProducer).sendEvent(Mockito.any());
        final InstrumentAckDTO instrumentAckDTO =
                new InstrumentAckDTO(
                        INITIATIVE_ID,
                        USER_ID,
                        WalletConstants.CHANNEL_APP_IO,
                        BRAND_LOGO,
                        BRAND_LOGO,
                        MASKED_PAN,
                        "REJECTED_ADD_INSTRUMENT",
                        TEST_DATE,
                        null);
        Mockito.when(timelineMapper.ackToTimeline(instrumentAckDTO)).thenReturn(TEST_OPERATION_DTO);

        try {
            walletService.processAck(instrumentAckDTO);
        } catch (WalletException e) {
            Assertions.fail();
        }
        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(0))
                .updateInstrumentNumber(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyInt(), Mockito.anyString());
        Mockito.verify(errorProducer, Mockito.times(0)).sendEvent(Mockito.any());
    }

    @Test
    void processAck_queue_error() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        Mockito.doThrow(new WalletException(400, ""))
                .when(timelineProducer)
                .sendEvent(Mockito.any(QueueOperationDTO.class));

        Mockito.doNothing().when(errorProducer).sendEvent(Mockito.any());

        final InstrumentAckDTO instrumentAckDTO =
                new InstrumentAckDTO(
                        INITIATIVE_ID,
                        USER_ID,
                        WalletConstants.CHANNEL_APP_IO,
                        BRAND_LOGO,
                        BRAND_LOGO,
                        MASKED_PAN,
                        "ADD_INSTRUMENT",
                        TEST_DATE,
                        1);
        Mockito.when(timelineMapper.ackToTimeline(instrumentAckDTO)).thenReturn(TEST_OPERATION_DTO);
        try {
            walletService.processAck(instrumentAckDTO);
        } catch (WalletException e) {
            Assertions.fail();
        }
        Mockito.verify(errorProducer, Mockito.times(1)).sendEvent(Mockito.any());
    }

    @Test
    void enrollInstrument_ko_initiative_after_end() {
        TEST_WALLET.setEndDate(LocalDate.MIN);
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        try {
            walletService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET);
            Assertions.fail();
        } catch (WalletException e) {
            assertEquals(HttpStatus.FORBIDDEN.value(), e.getCode());
            assertEquals(WalletConstants.ERROR_INITIATIVE_KO, e.getMessage());
        }
    }

    @Test
    void enrollInstrument_not_found() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.empty());

        try {
            walletService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET);
            Assertions.fail();
        } catch (WalletException e) {
            assertEquals(HttpStatus.NOT_FOUND.value(), e.getCode());
            assertEquals(WalletConstants.ERROR_WALLET_NOT_FOUND, e.getMessage());
        }
    }

    @Test
    void enrollInstrument_unsubscribed() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET_UNSUBSCRIBED));

        try {
            walletService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET);
            Assertions.fail();
        } catch (WalletException e) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), e.getCode());
            assertEquals(WalletConstants.ERROR_INITIATIVE_UNSUBSCRIBED, e.getMessage());
        }
    }

    @Test
    void deleteInstrument_ok() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name());
        TEST_WALLET.setNInstr(1);
        TEST_WALLET.setIban(null);
        TEST_WALLET.setEndDate(LocalDate.MAX);

        Mockito.doNothing()
                .when(paymentInstrumentRestConnector)
                .deleteInstrument(Mockito.any(DeactivationBodyDTO.class));

        Mockito.doNothing().when(timelineProducer).sendEvent(Mockito.any(QueueOperationDTO.class));

        try {
            walletService.deleteInstrument(INITIATIVE_ID, USER_ID, ID_WALLET);
        } catch (WalletException e) {
            Assertions.fail();
        }
        assertEquals(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name(), TEST_WALLET.getStatus());
        assertEquals(1, TEST_WALLET.getNInstr());
    }

    @Test
    void deleteInstrument_ko_initiative_after_end() {
        TEST_WALLET.setEndDate(LocalDate.MIN);
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        try {
            walletService.deleteInstrument(INITIATIVE_ID, USER_ID, INSTRUMENT_ID);
            Assertions.fail();
        } catch (WalletException e) {
            assertEquals(HttpStatus.FORBIDDEN.value(), e.getCode());
            assertEquals(WalletConstants.ERROR_INITIATIVE_KO, e.getMessage());
        }
    }

    @Test
    void deleteInstrument_ko_feignexception() {
        TEST_WALLET.setEndDate(LocalDate.MAX);
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        Request request =
                Request.create(Request.HttpMethod.PUT, "url", new HashMap<>(), null, new RequestTemplate());

        Mockito.doThrow(new FeignException.BadRequest("", request, new byte[0], null))
                .when(paymentInstrumentRestConnector)
                .deleteInstrument(Mockito.any(DeactivationBodyDTO.class));

        try {
            walletService.deleteInstrument(INITIATIVE_ID, USER_ID, INSTRUMENT_ID);
            Assertions.fail();
        } catch (WalletException e) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), e.getCode());
        }
    }

    @Test
    void deleteInstrument_not_found() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.empty());

        try {
            walletService.deleteInstrument(INITIATIVE_ID, USER_ID, INSTRUMENT_ID);
            Assertions.fail();
        } catch (WalletException e) {
            assertEquals(HttpStatus.NOT_FOUND.value(), e.getCode());
            assertEquals(WalletConstants.ERROR_WALLET_NOT_FOUND, e.getMessage());
        }
    }

    @Test
    void getEnrollmentStatus_ok() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        try {
            EnrollmentStatusDTO actual = walletService.getEnrollmentStatus(INITIATIVE_ID, USER_ID);
            assertEquals(TEST_WALLET.getStatus(), actual.getStatus());
        } catch (WalletException e) {
            Assertions.fail();
        }
    }

    @Test
    void getEnrollmentStatus_ko() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.empty());

        try {
            walletService.getEnrollmentStatus(INITIATIVE_ID, USER_ID);
            Assertions.fail();
        } catch (WalletException e) {
            assertEquals(HttpStatus.NOT_FOUND.value(), e.getCode());
            assertEquals(WalletConstants.ERROR_WALLET_NOT_FOUND, e.getMessage());
        }
    }

    @Test
    void enrollIban_ok_only_iban() {
        TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE.name());
        TEST_WALLET.setNInstr(0);
        TEST_WALLET.setEndDate(LocalDate.MAX);

        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));


        Mockito.doAnswer(
                        invocationOnMock -> {
                            TEST_WALLET.setIban(IBAN_OK);
                            TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_IBAN.name());
                            return null;
                        })
                .when(walletUpdatesRepositoryMock)
                .enrollIban(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.eq(IBAN_OK), Mockito.anyString());

        try {
            walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, CHANNEL, DESCRIPTION_OK);
        } catch (WalletException e) {
            Assertions.fail();
        }

        assertEquals(WalletStatus.NOT_REFUNDABLE_ONLY_IBAN.name(), TEST_WALLET.getStatus());
    }

    @Test
    void enrollIban_ok_error_queue() {
        TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE.name());
        TEST_WALLET.setNInstr(0);
        TEST_WALLET.setIban(null);
        TEST_WALLET.setEndDate(LocalDate.MAX);

        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        Mockito.doAnswer(
                        invocationOnMock -> {
                            TEST_WALLET.setIban(IBAN_OK);
                            TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_IBAN.name());
                            return null;
                        })
                .when(walletUpdatesRepositoryMock)
                .enrollIban(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.eq(IBAN_OK), Mockito.anyString());

        Mockito.doThrow(new WalletException(400, ""))
                .when(ibanProducer)
                .sendIban(Mockito.any(IbanQueueDTO.class));

        try {
            walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, CHANNEL, DESCRIPTION_OK);
        } catch (WalletException e) {
            Assertions.fail();
        }
        assertEquals(WalletStatus.NOT_REFUNDABLE_ONLY_IBAN.name(), TEST_WALLET.getStatus());
        Mockito.verify(errorProducer, Mockito.times(1)).sendEvent(Mockito.any());
    }

    @Test
    void enrollIban_ok_with_instrument() {
        TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name());
        TEST_WALLET.setNInstr(1);
        TEST_WALLET.setEndDate(LocalDate.MAX);

        Mockito.doNothing().when(ibanProducer).sendIban(Mockito.any(IbanQueueDTO.class));

        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));


        Mockito.doAnswer(
                        invocationOnMock -> {
                            TEST_WALLET.setIban(IBAN_OK);
                            TEST_WALLET.setStatus(WalletStatus.REFUNDABLE.name());
                            return null;
                        })
                .when(walletUpdatesRepositoryMock)
                .enrollIban(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.eq(IBAN_OK), Mockito.anyString());
        Mockito.doNothing().when(timelineProducer).sendEvent(Mockito.any(QueueOperationDTO.class));

        walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, CHANNEL, DESCRIPTION_OK);

        assertEquals(INITIATIVE_ID, TEST_WALLET.getInitiativeId());
        assertEquals(USER_ID, TEST_WALLET.getUserId());

        assertEquals(WalletStatus.REFUNDABLE.name(), TEST_WALLET.getStatus());
    }

    @Test
    void enrollIban_ok_idemp() {
        Mockito.doNothing().when(ibanProducer).sendIban(Mockito.any(IbanQueueDTO.class));
        TEST_WALLET.setIban(IBAN_OK);
        TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_IBAN.name());
        TEST_WALLET.setNInstr(0);
        TEST_WALLET.setEndDate(LocalDate.MAX);

        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));


        walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, CHANNEL, DESCRIPTION_OK);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(0))
                .enrollIban(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.eq(IBAN_OK), Mockito.anyString());
    }

    @Test
    void enrollIban_ko_iban_not_italian() {
        TEST_WALLET.setIban(null);
        TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name());
        TEST_WALLET.setEndDate(LocalDate.MAX);

        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        try {
            walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_KO_NOT_IT, CHANNEL, DESCRIPTION_OK);
            Assertions.fail();
        } catch (UnsupportedCountryException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    void enrollIban_ko_iban_wrong() {
        TEST_WALLET.setIban(null);
        TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name());
        TEST_WALLET.setEndDate(LocalDate.MAX);

        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        try {
            walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_WRONG, CHANNEL, DESCRIPTION_OK);
            Assertions.fail();
        } catch (IbanFormatException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    void enrollIban_ko_iban_digit_control() {
        TEST_WALLET.setIban(null);
        TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name());
        TEST_WALLET.setEndDate(LocalDate.MAX);

        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        try {
            walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_WRONG_DIGIT, CHANNEL, DESCRIPTION_OK);
            Assertions.fail();
        } catch (InvalidCheckDigitException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    void enrollIban_not_found() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.empty());

        try {
            walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, CHANNEL, DESCRIPTION_OK);
            Assertions.fail();
        } catch (WalletException e) {
            assertEquals(HttpStatus.NOT_FOUND.value(), e.getCode());
            assertEquals(WalletConstants.ERROR_WALLET_NOT_FOUND, e.getMessage());
        }
    }

    @Test
    void enrollIban_ko_unsubscribe() {
        TEST_WALLET.setEndDate(LocalDate.MAX);

        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET_UNSUBSCRIBED));

        try {
            walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, CHANNEL, DESCRIPTION_OK);
            Assertions.fail();
        } catch (WalletException e) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), e.getCode());
            assertEquals(WalletConstants.ERROR_INITIATIVE_UNSUBSCRIBED, e.getMessage());
        }
    }


    @Test
    void enrollIban_ko_initiative_after_end() {
        TEST_WALLET.setEndDate(LocalDate.MIN);

        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));


        try {
            walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, CHANNEL, DESCRIPTION_OK);
            Assertions.fail();
        } catch (WalletException e) {
            assertEquals(HttpStatus.FORBIDDEN.value(), e.getCode());
            assertEquals(WalletConstants.ERROR_INITIATIVE_KO, e.getMessage());
        }
    }

    @Test
    void enrollIban_update_ok() {
        Mockito.doNothing().when(ibanProducer).sendIban(Mockito.any(IbanQueueDTO.class));
        TEST_WALLET.setIban(IBAN_OK_OTHER);
        TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_IBAN.name());
        TEST_WALLET.setNInstr(0);
        TEST_WALLET.setEndDate(LocalDate.MAX);

        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));


        Mockito.doAnswer(
                        invocationOnMock -> {
                            TEST_WALLET.setIban(IBAN_OK);
                            return null;
                        })
                .when(walletUpdatesRepositoryMock)
                .enrollIban(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.eq(IBAN_OK), Mockito.anyString());

        walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, CHANNEL, DESCRIPTION_OK);

        assertEquals(WalletStatus.NOT_REFUNDABLE_ONLY_IBAN.name(), TEST_WALLET.getStatus());
    }

    @Test
    void enrollIban_ko_discountInitiative() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET_DISCOUNT));

        try {
            walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, CHANNEL, DESCRIPTION_OK);
            Assertions.fail();
        } catch (WalletException e) {
            assertEquals(HttpStatus.FORBIDDEN.value(), e.getCode());
            assertEquals(WalletConstants.ERROR_INITIATIVE_DISCOUNT_IBAN, e.getMessage());
        }
    }

    @Test
    void getWalletDetail_ok() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));
        TEST_WALLET.setIban(IBAN_OK);

        Mockito.when(walletMapper.toInitiativeDTO(Mockito.any(Wallet.class))).thenReturn(WALLET_DTO);
        try {
            WalletDTO actual = walletService.getWalletDetail(INITIATIVE_ID, USER_ID);
            assertEquals(WALLET_DTO.getInitiativeId(), actual.getInitiativeId());
            assertEquals(WALLET_DTO.getInitiativeName(), actual.getInitiativeName());
            assertEquals(WALLET_DTO.getStatus(), actual.getStatus());
            assertEquals(WALLET_DTO.getEndDate(), actual.getEndDate());
            assertEquals(WALLET_DTO.getIban(), actual.getIban());
            assertEquals(WALLET_DTO.getNInstr(), actual.getNInstr());
            assertEquals(WALLET_DTO.getAmount(), actual.getAmount());
            assertEquals(WALLET_DTO.getAccrued(), actual.getAccrued());
            assertEquals(WALLET_DTO.getRefunded(), actual.getRefunded());
        } catch (WalletException e) {
            Assertions.fail();
        }
    }

    @Test
    void getWalletDetail_ko() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.empty());
        try {
            walletService.getWalletDetail(INITIATIVE_ID, USER_ID);
            Assertions.fail();
        } catch (WalletException e) {
            assertEquals(HttpStatus.NOT_FOUND.value(), e.getCode());
            assertEquals(WalletConstants.ERROR_WALLET_NOT_FOUND, e.getMessage());
        }
    }

    @Test
    void getWalletDetail_issuer_ok() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET_ISSUER));

        Mockito.when(walletMapper.toIssuerInitiativeDTO(Mockito.any(Wallet.class)))
                .thenReturn(WALLET_ISSUER_DTO);
        try {
            WalletDTO actual = walletService.getWalletDetailIssuer(INITIATIVE_ID, USER_ID);
            assertEquals(WALLET_DTO.getAmount(), actual.getAmount());
            assertEquals(WALLET_DTO.getAccrued(), actual.getAccrued());
            assertEquals(WALLET_DTO.getRefunded(), actual.getRefunded());
        } catch (WalletException e) {
            Assertions.fail();
        }
    }

    @Test
    void getWalletDetail_issuer_ko() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.empty());
        try {
            walletService.getWalletDetailIssuer(INITIATIVE_ID, USER_ID);
            Assertions.fail();
        } catch (WalletException e) {
            assertEquals(HttpStatus.NOT_FOUND.value(), e.getCode());
            assertEquals(WalletConstants.ERROR_WALLET_NOT_FOUND, e.getMessage());
        }
    }

    @Test
    void getInitiativeList_ok() {
        TEST_WALLET.setIban(IBAN_OK);
        List<Wallet> walletList = new ArrayList<>();
        walletList.add(TEST_WALLET);

        Mockito.when(walletRepositoryMock.findByUserId(USER_ID)).thenReturn(walletList);
        Mockito.when(walletMapper.toInitiativeDTO(Mockito.any(Wallet.class))).thenReturn(WALLET_DTO);

        InitiativeListDTO initiativeListDto = walletService.getInitiativeList(USER_ID);

        assertFalse(initiativeListDto.getInitiativeList().isEmpty());

        WalletDTO actual = initiativeListDto.getInitiativeList().get(0);
        assertEquals(WALLET_DTO.getInitiativeId(), actual.getInitiativeId());
        assertEquals(WALLET_DTO.getInitiativeName(), actual.getInitiativeName());
        assertEquals(WALLET_DTO.getIban(), actual.getIban());
        assertEquals(WALLET_DTO.getStatus(), actual.getStatus());
    }

    @Test
    void createWalletOnbOk() {
        Mockito.when(walletMapper.map(Mockito.any())).thenReturn(TEST_WALLET);
        walletService.createWallet(EVALUATION_ONBOARDING_OK);
        Mockito.verify(walletRepositoryMock, Mockito.times(1)).save(Mockito.any());
        Mockito.verify(timelineProducer, Mockito.times(1)).sendEvent(Mockito.any());
    }

    @Test
    void createWalletJoined() {
        Mockito.when(walletMapper.map(Mockito.any())).thenReturn(TEST_WALLET);
        walletService.createWallet(EVALUATION_JOINED);
        Mockito.verify(walletRepositoryMock, Mockito.times(1)).save(Mockito.any());
        Mockito.verify(timelineProducer, Mockito.times(1)).sendEvent(Mockito.any());
    }

    @Test
    void createWallet_doNothing() {
        walletService.createWallet(OUTCOME_KO);
        Mockito.verify(walletRepositoryMock, Mockito.times(0)).save(Mockito.any());
        Mockito.verify(timelineProducer, Mockito.times(0)).sendEvent(Mockito.any());
    }

    @Test
    void createWallet_initiativeDiscount() {
        Mockito.when(walletMapper.map(Mockito.any())).thenReturn(TEST_WALLET);
        walletService.createWallet(OUTCOME_OK_DISCOUNT);
        Mockito.verify(walletRepositoryMock, Mockito.times(1)).save(Mockito.any());
        Mockito.verify(timelineProducer, Mockito.times(1)).sendEvent(Mockito.any());
        assertEquals(TEST_WALLET.getStatus(), WalletStatus.REFUNDABLE.name());
    }

    @Test
    void deleteOperation_error_queue() {
        IbanQueueWalletDTO iban =
                new IbanQueueWalletDTO(
                        USER_ID, INITIATIVE_ID, IBAN_OK, STATUS_KO, LocalDateTime.now().toString());
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        Mockito.doThrow(new WalletException(400, ""))
                .when(notificationProducer)
                .sendNotification(Mockito.any(NotificationQueueDTO.class));

        try {
            walletService.deleteOperation(iban);
        } catch (WalletException e) {
            Assertions.fail();
        }
        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(1))
                .deleteIban(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString());
        Mockito.verify(errorProducer, Mockito.times(1)).sendEvent(Mockito.any());
    }

    @Test
    void deleteOperation_ok() {
        TEST_WALLET.setIban(IBAN_OK);
        IbanQueueWalletDTO iban =
                new IbanQueueWalletDTO(
                        USER_ID, INITIATIVE_ID, IBAN_OK, STATUS_KO, LocalDateTime.now().toString());
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        Mockito.doNothing()
                .when(notificationProducer)
                .sendNotification(Mockito.any(NotificationQueueDTO.class));
        try {
            walletService.deleteOperation(iban);
        } catch (WalletException e) {
            Assertions.fail();
        }
        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(1))
                .deleteIban(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString());
    }

    @Test
    void deleteOperation_not_found() {
        IbanQueueWalletDTO iban =
                new IbanQueueWalletDTO(
                        USER_ID, INITIATIVE_ID, IBAN_OK, STATUS_KO, LocalDateTime.now().toString());
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.empty());

        try {
            walletService.deleteOperation(iban);
        } catch (WalletException e) {
            Assertions.fail();
        }

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(0))
                .deleteIban(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString());
    }

    @Test
    void deleteOperation_duplicate() {
        IbanQueueWalletDTO iban =
                new IbanQueueWalletDTO(
                        USER_ID, INITIATIVE_ID, IBAN_OK_OTHER, STATUS_KO, LocalDateTime.now().toString());
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        try {
            walletService.deleteOperation(iban);
        } catch (WalletException e) {
            Assertions.fail();
        }

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(0))
                .deleteIban(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString());
    }

    @Test
    void deleteOperation_status_ok() {
        IbanQueueWalletDTO iban =
                new IbanQueueWalletDTO(
                        USER_ID, INITIATIVE_ID, IBAN_OK, "OK", LocalDateTime.now().toString());

        walletService.deleteOperation(iban);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(0))
                .deleteIban(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString());
    }

    @Test
    void unsbubscribe_ok() {

        TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE.name());

        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        Mockito.doAnswer(
                        invocationOnMock -> {
                            TEST_WALLET.setRequestUnsubscribeDate(LocalDateTime.now());
                            TEST_WALLET.setStatus(WalletStatus.UNSUBSCRIBED);
                            return null;
                        })
                .when(walletRepositoryMock)
                .save(Mockito.any(Wallet.class));
        try {
            walletService.unsubscribe(INITIATIVE_ID, USER_ID);
            assertNotNull(TEST_WALLET.getRequestUnsubscribeDate());
            assertEquals(WalletStatus.UNSUBSCRIBED, TEST_WALLET.getStatus());
        } catch (WalletException e) {
            Assertions.fail();
        }
    }

    @Test
    void unsbubscribe_ko_unsubscribed() {

        TEST_WALLET.setStatus(WalletStatus.UNSUBSCRIBED);

        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        try {
            walletService.unsubscribe(INITIATIVE_ID, USER_ID);
        } catch (WalletException e) {
            Assertions.fail();
        }

        Mockito.verify(walletRepositoryMock, Mockito.times(0)).save(Mockito.any());
    }

    @Test
    void unsubscribe_ko() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.empty());
        try {
            walletService.unsubscribe(INITIATIVE_ID, USER_ID);
            Assertions.fail();
        } catch (WalletException e) {
            assertEquals(HttpStatus.NOT_FOUND.value(), e.getCode());
            assertEquals(WalletConstants.ERROR_WALLET_NOT_FOUND, e.getMessage());
        }
    }

    @Test
    void unsubscribe_ko_feignexception() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        Request request =
                Request.create(Request.HttpMethod.PUT, "url", new HashMap<>(), null, new RequestTemplate());

        Mockito.doThrow(new FeignException.BadRequest("", request, new byte[0], null))
                .when(paymentInstrumentRestConnector)
                .disableAllInstrument(Mockito.any(UnsubscribeCallDTO.class));

        try {
            walletService.unsubscribe(INITIATIVE_ID, USER_ID);
            Assertions.fail();
        } catch (WalletException e) {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getCode());
        }
    }

    @Test
    void processTransaction_ok() {
        Mockito.when(
                        walletUpdatesRepositoryMock.rewardTransaction(
                                Mockito.eq(INITIATIVE_ID),
                                Mockito.eq(USER_ID),
                                Mockito.any(),
                                Mockito.any(),
                                Mockito.any()))
                .thenReturn(TEST_WALLET);
        walletService.processTransaction(REWARD_TRX_DTO_REWARDED);
        Mockito.verify(timelineProducer, Mockito.times(1)).sendEvent(Mockito.any());
    }

    @Test
    void processTransaction_ko() {
        Mockito.when(
                        walletUpdatesRepositoryMock.rewardTransaction(
                                Mockito.eq(INITIATIVE_ID),
                                Mockito.eq(USER_ID),
                                Mockito.any(),
                                Mockito.any(),
                                Mockito.any()))
                .thenReturn(null);
        walletService.processTransaction(REWARD_TRX_DTO_REWARDED);
        Mockito.verify(timelineProducer, Mockito.times(0)).sendEvent(Mockito.any());
    }

    @Test
    void unsubscribe_rollback_wallet() {

        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        Request request =
                Request.create(Request.HttpMethod.PUT, "url", new HashMap<>(), null, new RequestTemplate());

        Mockito.doThrow(new FeignException.BadRequest("", request, new byte[0], null))
                .when(onboardingRestConnector)
                .disableOnboarding(Mockito.any(UnsubscribeCallDTO.class));

        try {
            walletService.unsubscribe(INITIATIVE_ID, USER_ID);
            Assertions.fail();
        } catch (WalletException e) {
            assertNull(TEST_WALLET.getRequestUnsubscribeDate());
            assertNotEquals(WalletStatus.UNSUBSCRIBED, TEST_WALLET.getStatus());
        }
    }

    @Test
    void unsubscribe_rollback_wallet_2() {

        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET_2));

        Request request =
                Request.create(Request.HttpMethod.PUT, "url", new HashMap<>(), null, new RequestTemplate());

        Mockito.doThrow(new FeignException.BadRequest("", request, new byte[0], null))
                .when(paymentInstrumentRestConnector)
                .disableAllInstrument(Mockito.any(UnsubscribeCallDTO.class));

        try {
            walletService.unsubscribe(INITIATIVE_ID, USER_ID);
            Assertions.fail();
        } catch (WalletException e) {
            assertNull(TEST_WALLET_2.getRequestUnsubscribeDate());
            assertNotEquals(WalletStatus.UNSUBSCRIBED, TEST_WALLET_2.getStatus());
        }
    }

    @Test
    void processTransaction_not_rewarded() {
        walletService.processTransaction(REWARD_TRX_DTO);
        Mockito.verify(walletRepositoryMock, Mockito.times(0)).save(Mockito.any());
        Mockito.verify(timelineProducer, Mockito.times(0)).sendEvent(Mockito.any());
    }

    @Test
    void update_wallet_ok() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name());
        TEST_WALLET.setNInstr(1);
        TEST_WALLET.setIban(null);

        Mockito.doNothing().when(timelineProducer).sendEvent(Mockito.any(QueueOperationDTO.class));
        Mockito.doAnswer(
                        invocationOnMock -> {
                            TEST_WALLET.setNInstr(0);
                            TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE.name());
                            return null;
                        })
                .when(walletUpdatesRepositoryMock)
                .decreaseInstrumentNumber(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString());

        try {
            List<WalletPIDTO> walletPIDTOList = new ArrayList<>();
            walletPIDTOList.add(new WalletPIDTO(INITIATIVE_ID, USER_ID, MASKED_PAN, BRAND_LOGO, BRAND_LOGO));
            WalletPIBodyDTO walletPIBodyDTO = new WalletPIBodyDTO(walletPIDTOList);
            walletService.updateWallet(walletPIBodyDTO);
        } catch (WalletException e) {
            Assertions.fail();
        }
        assertEquals(0, TEST_WALLET.getNInstr());
    }

    @Test
    void update_wallet_empty() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.empty());

        Mockito.doNothing().when(timelineProducer).sendEvent(Mockito.any(QueueOperationDTO.class));

        try {
            List<WalletPIDTO> walletPIDTOList = new ArrayList<>();
            walletPIDTOList.add(new WalletPIDTO(INITIATIVE_ID, USER_ID, MASKED_PAN, BRAND_LOGO, BRAND_LOGO));
            WalletPIBodyDTO walletPIBodyDTO = new WalletPIBodyDTO(walletPIDTOList);
            walletService.updateWallet(walletPIBodyDTO);
        } catch (WalletException e) {
            Assertions.fail();
        }
        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(0))
                .decreaseInstrumentNumber(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString());
    }

    @Test
    void update_wallet_ok_queue_error() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        Mockito.doAnswer(
                        invocationOnMock -> {
                            TEST_WALLET.setNInstr(0);
                            TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE.name());
                            return null;
                        })
                .when(walletUpdatesRepositoryMock)
                .decreaseInstrumentNumber(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString());

        Mockito.when(
                        timelineMapper.deleteInstrumentToTimeline(
                                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(TEST_OPERATION_DTO);

        Mockito.doThrow(new WalletException(400, ""))
                .when(timelineProducer)
                .sendEvent(Mockito.any(QueueOperationDTO.class));

        Mockito.doNothing().when(errorProducer).sendEvent(Mockito.any());

        TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name());
        TEST_WALLET.setNInstr(1);
        TEST_WALLET.setIban(null);

        try {
            List<WalletPIDTO> walletPIDTOList = new ArrayList<>();
            walletPIDTOList.add(new WalletPIDTO(INITIATIVE_ID, USER_ID, MASKED_PAN, BRAND_LOGO, BRAND_LOGO));
            WalletPIBodyDTO walletPIBodyDTO = new WalletPIBodyDTO(walletPIDTOList);
            walletService.updateWallet(walletPIBodyDTO);
        } catch (WalletException e) {
            Assertions.fail();
        }
        assertEquals(0, TEST_WALLET.getNInstr());
    }

    @Test
    void processRefund_fresh_history() {

        final RefundDTO dto =
                new RefundDTO(
                        "ID",
                        "EXT_ID",
                        "NOT_ID",
                        INITIATIVE_ID,
                        USER_ID,
                        "ORG_ID",
                        IBAN_OK,
                        "ACCEPTED",
                        REWARD_STATUS,
                        REFUND_TYPE,
                        4000L,
                        4000L,
                        START_DATE,
                        END_DATE,
                        LocalDateTime.now(),
                        null,
                        null,
                        1L,
                        LocalDate.now(),
                        TRANSFER_DATE,
                        NOTIFICATION_DATE,
                        "CRO");

        TEST_WALLET.setRefundHistory(null);
        TEST_WALLET.setRefunded(TEST_REFUNDED);

        Mockito.when(timelineMapper.refundToTimeline(dto)).thenReturn(TEST_OPERATION_DTO);

        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        walletService.processRefund(dto);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(1))
                .processRefund(
                        Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.any(), Mockito.any());
        Mockito.verify(timelineProducer, Mockito.times(1)).sendEvent(TEST_OPERATION_DTO);
        Mockito.verify(errorProducer, Mockito.times(0)).sendEvent(Mockito.any());
    }

    @Test
    void processRefund_queue_error() {

        final RefundDTO dto =
                new RefundDTO(
                        "ID",
                        "EXT_ID",
                        "NOT_ID",
                        INITIATIVE_ID,
                        USER_ID,
                        "ORG_ID",
                        IBAN_OK,
                        "ACCEPTED",
                        REWARD_STATUS,
                        REFUND_TYPE,
                        4000L,
                        4000L,
                        START_DATE,
                        END_DATE,
                        LocalDateTime.now(),
                        null,
                        null,
                        1L,
                        LocalDate.now(),
                        TRANSFER_DATE,
                        NOTIFICATION_DATE,
                        "CRO");

        TEST_WALLET.setRefundHistory(null);
        TEST_WALLET.setRefunded(TEST_REFUNDED);

        Mockito.when(timelineMapper.refundToTimeline(dto)).thenReturn(TEST_OPERATION_DTO);

        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        Mockito.doThrow(new WalletException(400, ""))
                .when(timelineProducer)
                .sendEvent(Mockito.any(QueueOperationDTO.class));

        walletService.processRefund(dto);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(1))
                .processRefund(
                        Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.any(), Mockito.any());
        Mockito.verify(errorProducer, Mockito.times(1)).sendEvent(Mockito.any());
    }

    @Test
    void processRefund_rejected() {

        final RefundDTO dto =
                new RefundDTO(
                        "ID",
                        "EXT_ID",
                        "NOT_ID",
                        INITIATIVE_ID,
                        USER_ID,
                        "ORG_ID",
                        IBAN_OK,
                        "REJECTED",
                        REWARD_STATUS,
                        REFUND_TYPE,
                        -4000L,
                        4000L,
                        START_DATE,
                        END_DATE,
                        LocalDateTime.now(),
                        null,
                        null,
                        2L,
                        LocalDate.now(),
                        TRANSFER_DATE,
                        NOTIFICATION_DATE,
                        "CRO");

        Map<String, RefundHistory> map = new HashMap<>();
        map.put("NOT_ID", new RefundHistory(1L));

        TEST_WALLET.setRefundHistory(map);
        TEST_WALLET.setRefunded(TEST_ACCRUED);

        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        Mockito.when(timelineMapper.refundToTimeline(dto)).thenReturn(TEST_OPERATION_DTO);

        walletService.processRefund(dto);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(1))
                .processRefund(
                        Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.any(), Mockito.any());
        Mockito.verify(timelineProducer, Mockito.times(1)).sendEvent(TEST_OPERATION_DTO);
        Mockito.verify(errorProducer, Mockito.times(0)).sendEvent(Mockito.any());
    }

    @Test
    void processRefund_skipped() {

        final RefundDTO dto =
                new RefundDTO(
                        "ID",
                        "EXT_ID",
                        "NOT_ID",
                        INITIATIVE_ID,
                        USER_ID,
                        "ORG_ID",
                        IBAN_OK,
                        "REJECTED",
                        REWARD_STATUS,
                        REFUND_TYPE,
                        0L,
                        4000L,
                        START_DATE,
                        END_DATE,
                        LocalDateTime.now(),
                        null,
                        null,
                        1L,
                        LocalDate.now(),
                        TRANSFER_DATE,
                        NOTIFICATION_DATE,
                        "CRO");

        Map<String, RefundHistory> map = new HashMap<>();
        map.put("NOT_ID", new RefundHistory(2L));

        TEST_WALLET.setRefundHistory(map);
        TEST_WALLET.setRefunded(TEST_ACCRUED);

        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        walletService.processRefund(dto);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(0))
                .processRefund(
                        Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.any(), Mockito.any());
        Mockito.verify(timelineProducer, Mockito.times(0))
                .sendEvent(Mockito.any(QueueOperationDTO.class));
        Mockito.verify(errorProducer, Mockito.times(0)).sendEvent(Mockito.any());
    }

    @Test
    void processRefund_rejected_not_accepted_before() {

        final RefundDTO dto =
                new RefundDTO(
                        "ID",
                        "EXT_ID",
                        "NOT_ID",
                        INITIATIVE_ID,
                        USER_ID,
                        "ORG_ID",
                        IBAN_OK,
                        "REJECTED",
                        REWARD_STATUS,
                        REFUND_TYPE,
                        0L,
                        4000L,
                        START_DATE,
                        END_DATE,
                        LocalDateTime.now(),
                        null,
                        null,
                        2L,
                        LocalDate.now(),
                        TRANSFER_DATE,
                        NOTIFICATION_DATE,
                        "CRO");

        TEST_WALLET.setRefundHistory(null);
        TEST_WALLET.setRefunded(BigDecimal.valueOf(0.00));

        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        walletService.processRefund(dto);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(1))
                .processRefund(
                        Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.any(), Mockito.any());
        Mockito.verify(timelineProducer, Mockito.times(0))
                .sendEvent(Mockito.any(QueueOperationDTO.class));
        Mockito.verify(errorProducer, Mockito.times(0)).sendEvent(Mockito.any());
    }

    @Test
    void processRefund_rejected_wallet_not_found() {

        final RefundDTO dto =
                new RefundDTO(
                        "ID",
                        "EXT_ID",
                        "NOT_ID",
                        INITIATIVE_ID,
                        USER_ID,
                        "ORG_ID",
                        IBAN_OK,
                        "REJECTED",
                        REWARD_STATUS,
                        REFUND_TYPE,
                        0L,
                        4000L,
                        START_DATE,
                        END_DATE,
                        LocalDateTime.now(),
                        null,
                        null,
                        2L,
                        LocalDate.now(),
                        TRANSFER_DATE,
                        NOTIFICATION_DATE,
                        "CRO");

        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.empty());

        walletService.processRefund(dto);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(0))
                .processRefund(
                        Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.any(), Mockito.any());
        Mockito.verify(timelineProducer, Mockito.times(0))
                .sendEvent(Mockito.any(QueueOperationDTO.class));
        Mockito.verify(errorProducer, Mockito.times(0)).sendEvent(Mockito.any());
    }

    @Test
    void enrollInstrumentIssuer_ok() {

        final InstrumentIssuerDTO instrument =
                new InstrumentIssuerDTO("hpan", CHANNEL, "VISA", "VISA", "***");

        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        TEST_WALLET.setIban(null);
        TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE.name());
        TEST_WALLET.setNInstr(0);
        TEST_WALLET.setEndDate(LocalDate.MAX);

        Mockito.doNothing()
                .when(paymentInstrumentRestConnector)
                .enrollInstrumentIssuer(Mockito.any(InstrumentIssuerCallDTO.class));

        try {
            walletService.enrollInstrumentIssuer(INITIATIVE_ID, USER_ID, instrument);
        } catch (WalletException e) {
            Assertions.fail();
        }
        assertEquals(WalletStatus.NOT_REFUNDABLE.name(), TEST_WALLET.getStatus());
        assertEquals(0, TEST_WALLET.getNInstr());
    }

    @Test
    void enrollInstrumentIssuer_discountInitiative() {
        final InstrumentIssuerDTO instrument =
                new InstrumentIssuerDTO("hpan", CHANNEL, "VISA", "VISA", "***");

        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET_DISCOUNT));

        try {
            walletService.enrollInstrumentIssuer(INITIATIVE_ID, USER_ID, instrument);
        } catch (WalletException e) {
            assertEquals(HttpStatus.FORBIDDEN.value(), e.getCode());
            assertEquals(WalletConstants.ERROR_INITIATIVE_DISCOUNT_PI, e.getMessage());
        }
    }

    @Test
    void enrollInstrumentIssuer_unsubscribed() {

        final InstrumentIssuerDTO instrument =
                new InstrumentIssuerDTO("hpan", CHANNEL, "VISA", "VISA", "***");

        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET_UNSUBSCRIBED));

        try {
            walletService.enrollInstrumentIssuer(INITIATIVE_ID, USER_ID, instrument);
        } catch (WalletException e) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), e.getCode());
            assertEquals(WalletConstants.ERROR_INITIATIVE_UNSUBSCRIBED, e.getMessage());
        }
    }

    @Test
    void enrollInstrumentIssuer_ko_feignexception() {

        TEST_WALLET.setEndDate(LocalDate.MAX);

        final InstrumentIssuerDTO instrument =
                new InstrumentIssuerDTO("hpan", CHANNEL, "VISA", "VISA", "***");

        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET_REFUNDABLE));


        Request request =
                Request.create(Request.HttpMethod.PUT, "url", new HashMap<>(), null, new RequestTemplate());

        Mockito.doThrow(new FeignException.BadRequest("", request, new byte[0], null))
                .when(paymentInstrumentRestConnector)
                .enrollInstrumentIssuer(Mockito.any(InstrumentIssuerCallDTO.class));

        try {
            walletService.enrollInstrumentIssuer(INITIATIVE_ID, USER_ID, instrument);
            Assertions.fail();
        } catch (WalletException e) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), e.getCode());
        }
    }

    @Test
    void getInitiativesWithInstrument_ok() {
        List<Wallet> walletList = new ArrayList<>();
        walletList.add(TEST_WALLET);
        walletList.add(TEST_WALLET_REFUNDABLE);
        walletList.add(TEST_WALLET_UNSUBSCRIBED);

        Mockito.when(walletRepositoryMock.findByUserId(USER_ID)).thenReturn(walletList);
        Mockito.when(walletMapper.toInitiativeDTO(TEST_WALLET)).thenReturn(WALLET_DTO);
        Mockito.when(walletMapper.toInitiativeDTO(TEST_WALLET_REFUNDABLE)).thenReturn(WALLET_REFUNDABLE_DTO);
        Mockito.when(walletMapper.toInitiativeDTO(TEST_WALLET_UNSUBSCRIBED)).thenReturn(WALLET_UNSUBSCRIBED_DTO);

        InitiativesStatusDTO instrOnInitiativeDTO = new InitiativesStatusDTO(WALLET_DTO.getInitiativeId(),
                WALLET_DTO.getInitiativeName(), null, WalletConstants.INSTRUMENT_STATUS_DEFAULT);
        InitiativesStatusDTO instrOnInitiativeRefDTO = new InitiativesStatusDTO(WALLET_REFUNDABLE_DTO.getInitiativeId(),
                WALLET_REFUNDABLE_DTO.getInitiativeName(), null, WalletConstants.INSTRUMENT_STATUS_DEFAULT);
        Mockito.when(walletMapper.toInstrStatusOnInitiativeDTO(WALLET_DTO)).thenReturn(instrOnInitiativeDTO);
        Mockito.when(walletMapper.toInstrStatusOnInitiativeDTO(WALLET_REFUNDABLE_DTO)).thenReturn(instrOnInitiativeRefDTO);

        List<StatusOnInitiativeDTO> initiativeList = new ArrayList<>();
        initiativeList.add(new StatusOnInitiativeDTO(INITIATIVE_ID, INSTRUMENT_ID, STATUS_ACTIVE));
        initiativeList.add(new StatusOnInitiativeDTO(INITIATIVE_ID + "PENDING", INSTRUMENT_ID, STATUS_PENDING_ENROLLMENT));
        Mockito.when(paymentInstrumentRestConnector.getInstrumentInitiativesDetail(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyList())).thenReturn(new InstrumentDetailDTO(MASKED_PAN, BRAND, initiativeList));

        List<InitiativesStatusDTO> instrStatusOnInitiative = new ArrayList<>();
        instrStatusOnInitiative.add(new InitiativesStatusDTO(TEST_WALLET.getInitiativeId(),
                TEST_WALLET.getInitiativeName(), INSTRUMENT_ID, STATUS_ACTIVE));
        instrStatusOnInitiative.add(new InitiativesStatusDTO(TEST_WALLET_REFUNDABLE.getInitiativeId(),
                TEST_WALLET_REFUNDABLE.getInitiativeName(), INSTRUMENT_ID, STATUS_PENDING_ENROLLMENT));
        InitiativesWithInstrumentDTO initiativesWithInstrumentDTO =
                new InitiativesWithInstrumentDTO(ID_WALLET, MASKED_PAN, BRAND, instrStatusOnInitiative);

        Mockito.when(walletMapper.toInstrumentOnInitiativesDTO(Mockito.anyString(), Mockito.any(), Mockito.anyList()))
                .thenReturn(initiativesWithInstrumentDTO);

        InitiativesWithInstrumentDTO result = walletService.getInitiativesWithInstrument(ID_WALLET, USER_ID);
        assertEquals(result.getIdWallet(), initiativesWithInstrumentDTO.getIdWallet());
        assertEquals(result.getMaskedPan(), initiativesWithInstrumentDTO.getMaskedPan());
        assertEquals(result.getBrand(), initiativesWithInstrumentDTO.getBrand());
        assertIterableEquals(result.getInitiativeList(), initiativesWithInstrumentDTO.getInitiativeList());
    }

    @Test
    void getInitiativesWithInstrument_noActiveInitiatives() {
        List<Wallet> walletList = new ArrayList<>();
        Mockito.when(walletRepositoryMock.findByUserId(USER_ID)).thenReturn(walletList);
        Mockito.when(paymentInstrumentRestConnector.getInstrumentInitiativesDetail(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyList())).thenReturn(new InstrumentDetailDTO(MASKED_PAN, BRAND, new ArrayList<>()));
        Mockito.when(walletMapper.toInstrumentOnInitiativesDTO(Mockito.anyString(), Mockito.any(), Mockito.anyList()))
                .thenReturn(new InitiativesWithInstrumentDTO(ID_WALLET, MASKED_PAN, BRAND, new ArrayList<>()));

        InitiativesWithInstrumentDTO result = walletService.getInitiativesWithInstrument(ID_WALLET, USER_ID);
        assertEquals(ID_WALLET, result.getIdWallet());
        assertEquals(MASKED_PAN, result.getMaskedPan());
        assertEquals(BRAND, result.getBrand());
        assertEquals(result.getInitiativeList(), new ArrayList<>());
    }

    @Test
    void getInitiativesWithInstrument_emptyListFromPI() {
        List<Wallet> walletList = new ArrayList<>();
        walletList.add(TEST_WALLET);
        walletList.add(TEST_WALLET_REFUNDABLE);
        walletList.add(TEST_WALLET_UNSUBSCRIBED);

        WalletDTO walletDtoRef = new WalletDTO(FAMILY_ID, INITIATIVE_ID_REFUNDABLE, INITIATIVE_NAME, WalletStatus.REFUNDABLE.name(),
                IBAN_OK, TEST_DATE_ONLY_DATE.minusDays(1), 0, TEST_AMOUNT, TEST_ACCRUED, TEST_REFUNDED,
                TEST_DATE, WalletConstants.INITIATIVE_REWARD_TYPE_REFUND, LOGO_URL, ORGANIZATION_NAME);
        WalletDTO walletDtoUnsub = new WalletDTO(FAMILY_ID, INITIATIVE_ID_REFUNDABLE, INITIATIVE_NAME, WalletStatus.REFUNDABLE.name(),
                IBAN_OK, TEST_DATE_ONLY_DATE.minusDays(1), 0, TEST_AMOUNT, TEST_ACCRUED, TEST_REFUNDED,
                TEST_DATE, WalletConstants.INITIATIVE_REWARD_TYPE_REFUND, LOGO_URL, ORGANIZATION_NAME);

        Mockito.when(walletRepositoryMock.findByUserId(USER_ID)).thenReturn(walletList);
        Mockito.when(walletMapper.toInitiativeDTO(TEST_WALLET)).thenReturn(WALLET_DTO);
        Mockito.when(walletMapper.toInitiativeDTO(TEST_WALLET_REFUNDABLE)).thenReturn(walletDtoRef);
        Mockito.when(walletMapper.toInitiativeDTO(TEST_WALLET_UNSUBSCRIBED)).thenReturn(walletDtoUnsub);

        InitiativesStatusDTO dtoTestWallet = new InitiativesStatusDTO(WALLET_DTO.getInitiativeId(),
                WALLET_DTO.getInitiativeName(), INSTRUMENT_ID, WalletConstants.INSTRUMENT_STATUS_DEFAULT);
        Mockito.when(walletMapper.toInstrStatusOnInitiativeDTO(WALLET_DTO)).thenReturn(dtoTestWallet);

        InstrumentDetailDTO instrDetailDTO = new InstrumentDetailDTO(MASKED_PAN, BRAND, new ArrayList<>());
        Mockito.when(paymentInstrumentRestConnector.getInstrumentInitiativesDetail(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyList())).thenReturn(instrDetailDTO);

        List<InitiativesStatusDTO> instrStatusOnInitiative = new ArrayList<>();
        instrStatusOnInitiative.add(new InitiativesStatusDTO(TEST_WALLET.getInitiativeId(),
                TEST_WALLET.getInitiativeName(), INSTRUMENT_ID, WalletConstants.INSTRUMENT_STATUS_DEFAULT));
        InitiativesWithInstrumentDTO initiativesWithInstrumentDTO =
                new InitiativesWithInstrumentDTO(ID_WALLET, MASKED_PAN, BRAND, instrStatusOnInitiative);

        Mockito.when(walletMapper.toInstrumentOnInitiativesDTO(Mockito.anyString(), Mockito.any(), Mockito.anyList()))
                .thenReturn(initiativesWithInstrumentDTO);

        InitiativesWithInstrumentDTO result = walletService.getInitiativesWithInstrument(ID_WALLET, USER_ID);
        assertEquals(result.getIdWallet(), initiativesWithInstrumentDTO.getIdWallet());
        assertEquals(result.getMaskedPan(), initiativesWithInstrumentDTO.getMaskedPan());
        assertEquals(result.getBrand(), initiativesWithInstrumentDTO.getBrand());
        assertIterableEquals(result.getInitiativeList(), initiativesWithInstrumentDTO.getInitiativeList());
    }

    @Test
    void getInitiativesWithInstrument_feignException() {
        List<Wallet> walletList = new ArrayList<>();
        walletList.add(TEST_WALLET);
        walletList.add(TEST_WALLET_REFUNDABLE);

        Mockito.when(walletRepositoryMock.findByUserId(USER_ID)).thenReturn(walletList);
        Mockito.when(walletMapper.toInitiativeDTO(TEST_WALLET)).thenReturn(WALLET_DTO);
        Mockito.when(walletMapper.toInitiativeDTO(TEST_WALLET_REFUNDABLE)).thenReturn(WALLET_REFUNDABLE_DTO);

        Request request =
                Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());
        Mockito.doThrow(new FeignException.BadRequest("", request, new byte[0], null))
                .when(paymentInstrumentRestConnector).getInstrumentInitiativesDetail(ID_WALLET, USER_ID, WalletConstants.FILTER_INSTRUMENT_STATUS_LIST);

        try {
            walletService.getInitiativesWithInstrument(ID_WALLET, USER_ID);
        } catch (WalletException e) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), e.getCode());
        }
    }

    @Test
    void suspend_ok() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        Mockito.doAnswer(
                        invocationOnMock -> {
                            TEST_WALLET.setStatus(WalletStatus.SUSPENDED);
                            return null;
                        })
                .when(walletUpdatesRepositoryMock)
                .suspendWallet(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString(), Mockito.any());

        Mockito.doNothing()
                .when(notificationProducer)
                .sendNotification(Mockito.any(NotificationQueueDTO.class));

        walletService.suspendWallet(INITIATIVE_ID, USER_ID);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(1))
                .suspendWallet(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString(), Mockito.any());
        Mockito.verify(onboardingRestConnector, Mockito.times(1))
                .suspendOnboarding(INITIATIVE_ID, USER_ID);
        Mockito.verify(notificationProducer, Mockito.times(1))
                .sendNotification(Mockito.any());
        assertEquals(WalletStatus.SUSPENDED, TEST_WALLET.getStatus());
    }

    @Test
    void suspend_walletUnsubscribed() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET_UNSUBSCRIBED));
        try {
            walletService.suspendWallet(INITIATIVE_ID, USER_ID);
        } catch (WalletException e) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), e.getCode());
            assertEquals(WalletConstants.ERROR_INITIATIVE_UNSUBSCRIBED, e.getMessage());
        }
    }

    @Test
    void suspend_ko() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        Mockito.doAnswer(
                        invocationOnMock -> {
                            TEST_WALLET.setStatus(WalletStatus.SUSPENDED);
                            return null;
                        })
                .when(walletUpdatesRepositoryMock)
                .suspendWallet(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString(), Mockito.any());

        Mockito.doThrow(new WalletException(500, WalletConstants.ERROR_MSG_HEADER_MESSAGE)).when(onboardingRestConnector)
                .suspendOnboarding(anyString(), anyString());

        try {
            walletService.suspendWallet(INITIATIVE_ID, USER_ID);
        } catch (WalletException e) {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getCode());
            assertEquals(WalletConstants.ERROR_MSG_HEADER_MESSAGE, e.getMessage());
        }
    }

    @Test
    void suspend_idemp() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET_SUSPENDED));

        walletService.suspendWallet(INITIATIVE_ID, USER_ID);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(0))
                .suspendWallet(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString(), Mockito.any());
        Mockito.verify(onboardingRestConnector, Mockito.times(0))
                .suspendOnboarding(INITIATIVE_ID, USER_ID);
    }

    @ParameterizedTest
    @ValueSource(strings = {WALLET_REFUNDABLE, WALLET_NOT_REFUNDABLE, WALLET_NOT_REFUNDABLE_ONLY_IBAN, WALLET_NOT_REFUNDABLE_ONLY_INSTRUMENT})
    void readmit_ok(String status) {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET_SUSPENDED));

        Mockito.doAnswer(
                        invocationOnMock -> {
                            TEST_WALLET_SUSPENDED.setStatus(status);
                            return null;
                        })
                .when(walletUpdatesRepositoryMock)
                .readmitWallet(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString(), Mockito.any());

        walletService.readmitWallet(INITIATIVE_ID, USER_ID);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(1))
                .readmitWallet(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString(), Mockito.any());
        Mockito.verify(onboardingRestConnector, Mockito.times(1))
                .readmitOnboarding(INITIATIVE_ID, USER_ID);
        assertEquals(status, TEST_WALLET_SUSPENDED.getStatus());
    }

    @Test
    void readmit_walletUnsubscribed() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET_UNSUBSCRIBED));
        try {
            walletService.readmitWallet(INITIATIVE_ID, USER_ID);
        } catch (WalletException e) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), e.getCode());
            assertEquals(WalletConstants.ERROR_INITIATIVE_UNSUBSCRIBED, e.getMessage());
        }
    }

    @Test
    void readmit_ko() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));

        Mockito.doAnswer(
                        invocationOnMock -> {
                            TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE.name());
                            return null;
                        })
                .when(walletUpdatesRepositoryMock)
                .readmitWallet(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString(), Mockito.any());

        Mockito.doThrow(new WalletException(500, WalletConstants.ERROR_MSG_HEADER_MESSAGE)).when(onboardingRestConnector)
                .readmitOnboarding(anyString(), anyString());

        try {
            walletService.readmitWallet(INITIATIVE_ID, USER_ID);
        } catch (WalletException e) {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getCode());
            assertEquals(WalletConstants.ERROR_MSG_HEADER_MESSAGE, e.getMessage());
        }
    }

    @Test
    void readmit_idemp() {
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET_REFUNDABLE));

        Mockito.doAnswer(
                        invocationOnMock -> {
                            TEST_WALLET.setStatus(WalletStatus.REFUNDABLE.name());
                            return null;
                        })
                .when(walletUpdatesRepositoryMock)
                .readmitWallet(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString(), Mockito.any());

        walletService.readmitWallet(INITIATIVE_ID, USER_ID);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(1))
                .readmitWallet(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString(), Mockito.any());
        Mockito.verify(onboardingRestConnector, Mockito.times(1))
                .readmitOnboarding(INITIATIVE_ID, USER_ID);
        assertEquals(WalletStatus.REFUNDABLE.name(), TEST_WALLET_REFUNDABLE.getStatus());
    }
}
