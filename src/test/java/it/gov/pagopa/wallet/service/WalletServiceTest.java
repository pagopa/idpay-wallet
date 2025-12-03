package it.gov.pagopa.wallet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClientException;
import it.gov.pagopa.common.config.ObjectMapperConfig;
import it.gov.pagopa.wallet.connector.*;
import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.*;
import it.gov.pagopa.wallet.dto.mapper.TimelineMapper;
import it.gov.pagopa.wallet.dto.mapper.WalletMapper;
import it.gov.pagopa.wallet.dto.payment.TransactionBarCodeCreationRequest;
import it.gov.pagopa.wallet.dto.payment.TransactionBarCodeEnrichedResponse;
import it.gov.pagopa.wallet.enums.*;
import it.gov.pagopa.wallet.event.producer.ErrorProducer;
import it.gov.pagopa.wallet.event.producer.IbanProducer;
import it.gov.pagopa.wallet.event.producer.NotificationProducer;
import it.gov.pagopa.wallet.event.producer.TimelineProducer;
import it.gov.pagopa.wallet.exception.WalletUpdateException;
import it.gov.pagopa.wallet.exception.custom.*;
import it.gov.pagopa.wallet.model.Wallet;
import it.gov.pagopa.wallet.model.Wallet.RefundHistory;
import it.gov.pagopa.wallet.repository.WalletRepository;
import it.gov.pagopa.wallet.repository.WalletUpdatesRepository;
import it.gov.pagopa.wallet.utils.AuditUtilities;
import it.gov.pagopa.wallet.utils.Utilities;
import lombok.SneakyThrows;
import org.iban4j.IbanFormatException;
import org.iban4j.InvalidCheckDigitException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Stream;

import static it.gov.pagopa.wallet.constants.WalletConstants.CHANNEL_APP_IO;
import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.*;
import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionMessage.*;
import static it.gov.pagopa.wallet.constants.WalletConstants.STATUS_KO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = {WalletServiceImpl.class, ObjectMapperConfig.class})
@TestPropertySource(
        locations = "classpath:application.yml",
        properties = {
                "app.iban.formalControl=true",
                "app.delete.paginationSize=100",
                "app.delete.delayTime=1000"
        })
class WalletServiceTest {
    @MockitoBean
    IbanProducer ibanProducer;
    @MockitoBean
    TimelineProducer timelineProducer;
    @MockitoBean
    ErrorProducer errorProducer;
    @MockitoBean
    NotificationProducer notificationProducer;
    @MockitoBean
    WalletRepository walletRepositoryMock;
    @MockitoBean
    WalletUpdatesRepository walletUpdatesRepositoryMock;
    @MockitoBean
    PaymentInstrumentRestConnector paymentInstrumentRestConnector;
    @MockitoBean
    OnboardingRestConnector onboardingRestConnector;
    @MockitoBean
    InitiativeRestConnector initiativeRestConnector;
    @MockitoBean
    WalletMapper walletMapper;
    @MockitoBean
    TimelineMapper timelineMapper;
    @Autowired
    WalletService walletService;
    @MockitoBean
    AuditUtilities auditUtilities;
    @MockitoBean
    PaymentRestConnector paymentRestConnector;
    @Autowired
    ObjectMapper objectMapper;

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
    private static final String ID_WALLET = "TEST_USER_ID_TEST_INITIATIVE_ID";
    private static final String INSTRUMENT_ID = "TEST_INSTRUMENT_ID";
    private static final String IBAN_OK = "IT09P3608105138205493205495";
    private static final String IBAN_OK_OTHER = "IT09P3608105138205493205494";
    private static final String IBAN_KO_NOT_IT = "GB29NWBK60161331926819";
    private static final String IBAN_WRONG = "it99C1234567890123456789012222";
    private static final String IBAN_WRONG_DIGIT = "IT09P3608105138205493205496";
    private static final String DESCRIPTION_OK = "conto cointestato";
    private static final LocalDateTime TEST_DATE = LocalDateTime.now();
    private static final String USERMAIL = "USERMAIL";
    private static final String NAME = "NAME";
    private static final String SURNAME = "SURNAME";
    public static final InstrumentAckDTO INSTRUMENT_ACK_DTO_REJECTED_INSTRUMENT = new InstrumentAckDTO(
            INITIATIVE_ID,
            USER_ID,
            WalletConstants.CHANNEL_APP_IO,
            WalletConstants.INSTRUMENT_TYPE_CARD,
            BRAND_LOGO,
            BRAND_LOGO,
            MASKED_PAN,
            "REJECTED_ADD_INSTRUMENT",
            TEST_DATE,
            null);
    public static final InstrumentAckDTO INSTRUMENT_ACK_DTO_ADD_INSTRUMENT = new InstrumentAckDTO(
            INITIATIVE_ID,
            USER_ID,
            WalletConstants.CHANNEL_APP_IO,
            WalletConstants.INSTRUMENT_TYPE_CARD,
            BRAND_LOGO,
            BRAND_LOGO,
            MASKED_PAN,
            "ADD_INSTRUMENT",
            TEST_DATE,
            1);
    private static final LocalDateTime TEST_SUSPENSION_DATE = LocalDateTime.now().minusDays(1);
    private static final LocalDate TEST_DATE_ONLY_DATE = LocalDate.now();
    private static final LocalDate TEST_END_DATE = LocalDate.now().plusDays(2);
    private static final Long TEST_AMOUNT = 200L;
    private static final Long TEST_ACCRUED = 4000L;
    private static final Long TEST_REFUNDED = 0L;
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
    private static final String INITIATIE_REWARD_TYPE_REFUND = "REFUND";
    private static final String OPERATION_TYPE_DELETE_INITIATIVE = "DELETE_INITIATIVE";
    private static final int PAGINATION_VALUE = 100;
    private static final Long COUNTER_VERSION = 0L;
    private static final List<Long> COUNTER_HISTORY = new ArrayList<>();
    private static final Long TEST_VERSION = 1L;
    private static final String SERVICE_ID = "serviceid";
    private static Wallet testWallet =
            Wallet.builder()
                    .build();

    private static final Wallet TEST_WALLET_FAMILY =
            Wallet.builder()
                    .userId(USER_ID)
                    .familyId(FAMILY_ID)
                    .initiativeId(INITIATIVE_ID)
                    .initiativeName(INITIATIVE_NAME)
                    .acceptanceDate(TEST_DATE)
                    .status(WalletStatus.NOT_REFUNDABLE.name())
                    .initiativeEndDate(TEST_DATE_ONLY_DATE)
                    .amountCents(TEST_AMOUNT)
                    .accruedCents(TEST_ACCRUED)
                    .refundedCents(TEST_REFUNDED)
                    .lastCounterUpdate(TEST_DATE)
                    .counterVersion(COUNTER_VERSION)
                    .counterHistory(COUNTER_HISTORY)
                    .build();

    private static final Wallet TEST_WALLET_FAMILY_OLD_COUNTER_NOT_IN_HISTORY =
            Wallet.builder()
                    .userId(USER_ID)
                    .familyId(FAMILY_ID)
                    .initiativeId(INITIATIVE_ID)
                    .initiativeName(INITIATIVE_NAME)
                    .acceptanceDate(TEST_DATE)
                    .status(WalletStatus.NOT_REFUNDABLE.name())
                    .initiativeEndDate(TEST_DATE_ONLY_DATE)
                    .amountCents(TEST_AMOUNT)
                    .accruedCents(TEST_ACCRUED)
                    .refundedCents(TEST_REFUNDED)
                    .lastCounterUpdate(TEST_DATE)
                    .counterVersion(TEST_VERSION)
                    .counterHistory(COUNTER_HISTORY)
                    .build();

    private static final Wallet TEST_WALLET_FAMILY_COUNTERS_NOT_VALID =
            Wallet.builder()
                    .userId(USER_ID)
                    .familyId(FAMILY_ID)
                    .initiativeId(INITIATIVE_ID)
                    .initiativeName(INITIATIVE_NAME)
                    .acceptanceDate(TEST_DATE)
                    .status(WalletStatus.NOT_REFUNDABLE.name())
                    .initiativeEndDate(TEST_DATE_ONLY_DATE)
                    .amountCents(TEST_AMOUNT)
                    .accruedCents(TEST_ACCRUED)
                    .refundedCents(TEST_REFUNDED)
                    .lastCounterUpdate(TEST_DATE)
                    .counterVersion(TEST_VERSION)
                    .counterHistory(List.of(TEST_VERSION))
                    .build();

    private static final Wallet TEST_WALLET_DISCOUNT =
            Wallet.builder()
                    .userId(USER_ID)
                    .initiativeId(INITIATIVE_ID)
                    .initiativeName(INITIATIVE_NAME)
                    .acceptanceDate(TEST_DATE)
                    .status(WalletStatus.NOT_REFUNDABLE.name())
                    .initiativeEndDate(TEST_DATE_ONLY_DATE)
                    .amountCents(TEST_AMOUNT)
                    .accruedCents(TEST_ACCRUED)
                    .refundedCents(TEST_REFUNDED)
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
                    .initiativeEndDate(TEST_DATE_ONLY_DATE)
                    .amountCents(TEST_AMOUNT)
                    .accruedCents(TEST_ACCRUED)
                    .refundedCents(TEST_REFUNDED)
                    .lastCounterUpdate(TEST_DATE)
                    .initiativeRewardType(INITIATIE_REWARD_TYPE_REFUND)
                    .build();
    private static final Wallet TEST_WALLET_UNSUBSCRIBED =
            Wallet.builder()
                    .userId(USER_ID)
                    .initiativeId(INITIATIVE_ID_UNSUBSCRIBED)
                    .initiativeName(INITIATIVE_NAME)
                    .acceptanceDate(TEST_DATE)
                    .status(WalletStatus.UNSUBSCRIBED)
                    .initiativeEndDate(TEST_DATE_ONLY_DATE)
                    .amountCents(TEST_AMOUNT)
                    .accruedCents(TEST_ACCRUED)
                    .refundedCents(TEST_REFUNDED)
                    .build();

    private static final Wallet TEST_WALLET_SUSPENDED =
            Wallet.builder()
                    .userId(USER_ID)
                    .initiativeId(INITIATIVE_ID)
                    .initiativeName(INITIATIVE_NAME)
                    .acceptanceDate(TEST_DATE)
                    .status(WalletStatus.SUSPENDED)
                    .initiativeEndDate(TEST_DATE_ONLY_DATE)
                    .amountCents(TEST_AMOUNT)
                    .accruedCents(TEST_ACCRUED)
                    .refundedCents(TEST_REFUNDED)
                    .lastCounterUpdate(TEST_DATE)
                    .updateDate(TEST_DATE)
                    .suspensionDate(TEST_SUSPENSION_DATE)
                    .initiativeRewardType(INITIATIE_REWARD_TYPE_REFUND)
                    .build();

    private static final Wallet TEST_WALLET_ISSUER =
            Wallet.builder().amountCents(TEST_AMOUNT).accruedCents(TEST_ACCRUED).refundedCents(TEST_REFUNDED).build();

    private static final QueueOperationDTO TEST_OPERATION_DTO =
            QueueOperationDTO.builder().userId(USER_ID).build();

    private static final Wallet TEST_WALLET_2 =
            Wallet.builder()
                    .userId(USER_ID)
                    .initiativeId(INITIATIVE_ID)
                    .initiativeName(INITIATIVE_NAME)
                    .acceptanceDate(TEST_DATE)
                    .status(WalletStatus.NOT_REFUNDABLE.name())
                    .initiativeEndDate(TEST_DATE_ONLY_DATE)
                    .amountCents(TEST_AMOUNT)
                    .accruedCents(TEST_ACCRUED)
                    .refundedCents(TEST_REFUNDED)
                    .lastCounterUpdate(TEST_DATE)
                    .build();

    private static final WalletDTO WALLET_DTO =
            new WalletDTO(
                    FAMILY_ID,
                    INITIATIVE_ID,
                    INITIATIVE_NAME,
                    WalletStatus.NOT_REFUNDABLE.name(),
                    VoucherStatus.EXPIRED.name(),
                    IBAN_OK,
                    TEST_DATE_ONLY_DATE,
                    TEST_DATE_ONLY_DATE,
                    TEST_END_DATE,
                    0,
                    TEST_AMOUNT,
                    TEST_AMOUNT,
                    TEST_ACCRUED,
                    TEST_REFUNDED,
                    TEST_DATE,
                    WalletConstants.INITIATIVE_REWARD_TYPE_REFUND,
                    LOGO_URL,
                    ORGANIZATION_NAME,
                    10L,
                    100L,
                    COUNTER_VERSION,
                    COUNTER_HISTORY,
                    SERVICE_ID,
                    USERMAIL,
                    Channel.WEB,
                    NAME,
                    SURNAME);

    private static final WalletDTO WALLET_REFUNDABLE_DTO =
            new WalletDTO(
                    FAMILY_ID,
                    INITIATIVE_ID_REFUNDABLE,
                    INITIATIVE_NAME,
                    WalletStatus.REFUNDABLE.name(),
                    VoucherStatus.ACTIVE.name(),
                    IBAN_OK,
                    TEST_END_DATE,
                    TEST_DATE_ONLY_DATE,
                    TEST_END_DATE,
                    0,
                    TEST_AMOUNT,
                    TEST_AMOUNT,
                    TEST_ACCRUED,
                    TEST_REFUNDED,
                    TEST_DATE,
                    WalletConstants.INITIATIVE_REWARD_TYPE_REFUND,
                    LOGO_URL,
                    ORGANIZATION_NAME,
                    10L,
                    100L,
                    COUNTER_VERSION,
                    COUNTER_HISTORY,
                    SERVICE_ID,
                    USERMAIL,
                    Channel.WEB,
                    NAME,
                    SURNAME);

    private static final WalletDTO WALLET_UNSUBSCRIBED_DTO =
            new WalletDTO(
                    FAMILY_ID,
                    INITIATIVE_ID_UNSUBSCRIBED,
                    INITIATIVE_NAME,
                    WalletStatus.UNSUBSCRIBED,
                    VoucherStatus.EXPIRED.name(),
                    IBAN_OK,
                    TEST_END_DATE,
                    TEST_DATE_ONLY_DATE,
                    TEST_END_DATE,
                    0,
                    TEST_AMOUNT,
                    TEST_AMOUNT,
                    TEST_ACCRUED,
                    TEST_REFUNDED,
                    TEST_DATE,
                    WalletConstants.INITIATIVE_REWARD_TYPE_REFUND,
                    LOGO_URL,
                    ORGANIZATION_NAME,
                    10L,
                    100L,
                    COUNTER_VERSION,
                    COUNTER_HISTORY,
                    SERVICE_ID,
                    USERMAIL,
                    Channel.WEB,
                    NAME,
                    SURNAME);

    private static final WalletDTO WALLET_ISSUER_DTO =
            new WalletDTO(null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    0,
                    TEST_AMOUNT,
                    TEST_AMOUNT,
                    TEST_ACCRUED,
                    TEST_REFUNDED,
                    TEST_DATE,
                    WalletConstants.INITIATIVE_REWARD_TYPE_REFUND,
                    LOGO_URL,
                    ORGANIZATION_NAME,
                    null,
                    null,
                    COUNTER_VERSION,
                    COUNTER_HISTORY,
                    SERVICE_ID,
                    USERMAIL,
                    Channel.WEB,
                    NAME,
                    SURNAME);

    private static final RewardDTO REWARD_DTO =
            RewardDTO.builder()
                    .accruedRewardCents(TEST_AMOUNT)
                    .counters(new Counters(false, 1L, TEST_AMOUNT, TEST_AMOUNT, TEST_ACCRUED, TEST_VERSION))
                    .build();

    private static final RewardTransactionDTO REWARD_TRX_DTO_REWARDED =
            RewardTransactionDTO.builder()
                    .userId(USER_ID)
                    .channel("RTD")
                    .status("REWARDED")
                    .rewards(Map.of(INITIATIVE_ID, REWARD_DTO))
                    .build();

    private static final RewardTransactionDTO REWARD_TRX_DTO_EXPIRED =
            RewardTransactionDTO.builder()
                    .userId(USER_ID)
                    .initiativeId(INITIATIVE_ID)
                    .channel("BARCODE")
                    .status("EXPIRED")
                    .extendedAuthorization(true)
                    .rewards(Collections.emptyMap())
                    .build();

    private static final RewardTransactionDTO REWARD_TRX_DTO_REFUNDED =
            RewardTransactionDTO.builder()
                    .userId(USER_ID)
                    .initiativeId(INITIATIVE_ID)
                    .channel("BARCODE")
                    .status("REFUNDED")
                    .extendedAuthorization(true)
                    .rewards(Collections.emptyMap())
                    .build();

    private static final RewardTransactionDTO REWARD_TRX_DTO_SYNC_REWARDED =
            RewardTransactionDTO.builder()
                    .userId(USER_ID)
                    .channel("QRCODE")
                    .status("REWARDED")
                    .rewards(Map.of(INITIATIVE_ID, REWARD_DTO))
                    .build();

    private static final RewardTransactionDTO REWARD_TRX_DTO_SYNC_CAPTURED_REWARDS =
            RewardTransactionDTO.builder()
                    .userId(USER_ID)
                    .channel("QRCODE")
                    .status("CAPTURED")
                    .rewards(Map.of(INITIATIVE_ID, REWARD_DTO))
                    .build();

    private static final RewardTransactionDTO REWARD_TRX_DTO_SYNC_CAPTURED_NOREWARDS =
            RewardTransactionDTO.builder()
                    .userId(USER_ID)
                    .channel("QRCODE")
                    .status("CAPTURED")
                    .rewards(Collections.emptyMap())
                    .build();

    private static final RewardTransactionDTO REWARD_TRX_DTO =
            RewardTransactionDTO.builder()
                    .channel("RTD")
                    .status("NOT_REWARDED")
                    .rewards(Map.of(INITIATIVE_ID, new RewardDTO()))
                    .build();

    private static final RewardTransactionDTO REWARD_TRX_DTO_SYNC_NOT_AUTH =
            RewardTransactionDTO.builder()
                    .channel("QRCODE")
                    .status("NOT_REWARDED")
                    .rewards(Map.of(INITIATIVE_ID, new RewardDTO()))
                    .build();

    private static final RewardTransactionDTO REWARD_TRX_DTO_AUTH =
            RewardTransactionDTO.builder()
                    .channel("RTD")
                    .status("AUTHORIZED")
                    .rewards(Map.of(INITIATIVE_ID, new RewardDTO()))
                    .build();

    private static final RewardTransactionDTO REWARD_TRX_DTO_SYNC_AUTHORIZED =
            RewardTransactionDTO.builder()
                    .userId(USER_ID)
                    .channel("QRCODE")
                    .status("AUTHORIZED")
                    .rewards(Map.of(INITIATIVE_ID, REWARD_DTO))
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
                    500L,
                    WalletConstants.INITIATIVE_REWARD_TYPE_REFUND,
                    ORGANIZATION_NAME,
                    Boolean.FALSE,
                    100L,
                    SERVICE_ID,
                    Channel.IO,
                    USERMAIL,
                    NAME,
                    SURNAME)
            ;

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
                    500L,
                    WalletConstants.INITIATIVE_REWARD_TYPE_REFUND,
                    ORGANIZATION_NAME,
                    Boolean.FALSE,
                    100L,
                    SERVICE_ID,
                    Channel.IO,
                    USERMAIL,
                    NAME,
                    SURNAME);

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
                    500L,
                    WalletConstants.INITIATIVE_REWARD_TYPE_REFUND,
                    ORGANIZATION_NAME,
                    Boolean.FALSE,
                    100L,
                    SERVICE_ID,
                    Channel.IO,
                    USERMAIL,
                    NAME,
                    SURNAME);

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
                    500L,
                    WalletConstants.INITIATIVE_REWARD_TYPE_DISCOUNT,
                    ORGANIZATION_NAME,
                    Boolean.FALSE,
                    100L,
                    SERVICE_ID,
                    Channel.IO,
                    USERMAIL,
                    NAME,
                    SURNAME);

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        MockitoAnnotations.openMocks(this);
        testWallet = Wallet.builder()
                .userId(USER_ID)
                .initiativeId(INITIATIVE_ID)
                .initiativeName(INITIATIVE_NAME)
                .acceptanceDate(TEST_DATE)
                .status(WalletStatus.NOT_REFUNDABLE.name())
                .initiativeEndDate(TEST_DATE_ONLY_DATE)
                .amountCents(TEST_AMOUNT)
                .accruedCents(TEST_ACCRUED)
                .refundedCents(TEST_REFUNDED)
                .lastCounterUpdate(TEST_DATE)
                .initiativeRewardType(INITIATIE_REWARD_TYPE_REFUND)
                .counterVersion(COUNTER_VERSION)
                .build();
        Mockito.when(paymentRestConnector.createExtendedTransaction(
                any(TransactionBarCodeCreationRequest.class),
                anyString()
        )).thenReturn(mockEnrichedResponse());
    }

    private TransactionBarCodeEnrichedResponse mockEnrichedResponse() {
        TransactionBarCodeEnrichedResponse r = new TransactionBarCodeEnrichedResponse();
        r.setId("trx-id-1");
        r.setTrxCode("ABC123");
        r.setInitiativeId(INITIATIVE_ID);
        r.setInitiativeName(INITIATIVE_ID);
        r.setStatus(SyncTrxStatus.CREATED);
        r.setTrxExpirationSeconds(3600L);
        r.setResidualBudgetCents(0L);

        // Date fisse per stabilità dei test
        r.setTrxDate(OffsetDateTime.parse("2025-09-24T10:00:00+00:00"));
        r.setTrxEndDate(OffsetDateTime.parse("2025-10-24T10:00:00+00:00"));
        return r;
    }

    @Value("${app.delete.paginationSize}")
    private String pagination;

    @Test
    void enrollInstrument_ok() {
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        testWallet.setIban(null);
        testWallet.setStatus(WalletStatus.NOT_REFUNDABLE.name());
        testWallet.setNInstr(0);
        testWallet.setInitiativeEndDate(LocalDate.MAX);

        Mockito.doNothing()
                .when(paymentInstrumentRestConnector)
                .enrollInstrument(any(InstrumentCallBodyDTO.class));


        walletService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL_APP_IO);

        assertEquals(WalletStatus.NOT_REFUNDABLE.name(), testWallet.getStatus());
        assertEquals(0, testWallet.getNInstr());
    }

    @Test
    void enrollInstrument_ko_paymentInstrumentConnectorException() {
        // Given
        testWallet.setInitiativeEndDate(LocalDate.MAX);
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        Mockito.when(timelineMapper.ackToTimeline(INSTRUMENT_ACK_DTO_REJECTED_INSTRUMENT)).thenReturn(TEST_OPERATION_DTO);

        doThrow(new PaymentInstrumentNotFoundException(PAYMENT_INSTRUMENT_NOT_FOUND_MSG))
                .when(paymentInstrumentRestConnector)
                .enrollInstrument(any(InstrumentCallBodyDTO.class));

        // When
        PaymentInstrumentNotFoundException exception = assertThrows(PaymentInstrumentNotFoundException.class,
                () -> walletService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL_APP_IO));

        // Then
        assertEquals(PAYMENT_INSTRUMENT_NOT_FOUND, exception.getCode());
        verify(timelineProducer, times(1)).sendEvent(any());
    }

    @Test
    void enrollInstrument_ko_discountInitiative() {
        // Given
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(TEST_WALLET_DISCOUNT));

        // When
        EnrollmentNotAllowedException exception = assertThrows(EnrollmentNotAllowedException.class,
                () -> walletService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL_APP_IO));

        // Then
        assertEquals(ENROLL_INSTRUMENT_DISCOUNT_INITIATIVE, exception.getCode());
        assertEquals(PAYMENT_INSTRUMENT_ENROLL_NOT_ALLOWED_DISCOUNT_MSG, exception.getMessage());
        verifyNoInteractions(paymentInstrumentRestConnector);
        verifyNoInteractions(timelineProducer);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DISCOUNT", "REFUND"})
    void processAck(String initiativeRewardType) {
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        testWallet.setNInstr(0);
        testWallet.setIban(null);
        testWallet.setInitiativeRewardType(initiativeRewardType);
        testWallet.setStatus(WalletStatus.SUSPENDED);

        Mockito.doNothing().when(timelineProducer).sendEvent(any(QueueOperationDTO.class));
        Mockito.doNothing().when(errorProducer).sendEvent(any());
        Mockito.when(timelineMapper.ackToTimeline(INSTRUMENT_ACK_DTO_ADD_INSTRUMENT)).thenReturn(TEST_OPERATION_DTO);

        walletService.processAck(INSTRUMENT_ACK_DTO_ADD_INSTRUMENT);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(1))
                .updateInstrumentNumber(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyInt(), Mockito.anyString());
        Mockito.verify(errorProducer, Mockito.times(0)).sendEvent(any());
    }

    @Test
    void processAck_not_found() {
        //Given
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.empty());

        Mockito.when(timelineMapper.ackToTimeline(INSTRUMENT_ACK_DTO_ADD_INSTRUMENT)).thenReturn(TEST_OPERATION_DTO);

        // When
        UserNotOnboardedException exception = assertThrows(UserNotOnboardedException.class,
                () -> walletService.processAck(INSTRUMENT_ACK_DTO_ADD_INSTRUMENT));

        // Then
        assertEquals(USER_NOT_ONBOARDED, exception.getCode());
        assertEquals(String.format(String.format(USER_NOT_ONBOARDED_MSG, INSTRUMENT_ACK_DTO_ADD_INSTRUMENT.getInitiativeId())),
                exception.getMessage());

        verify(walletRepositoryMock, times(1)).findById(any());
        verifyNoMoreInteractions(walletRepositoryMock);
        verifyNoInteractions(timelineMapper);
        verifyNoInteractions(timelineProducer);
    }

    @Test
    void processAck_ko() {
        Mockito.when(walletRepositoryMock.findById(Mockito.anyString())).thenReturn(Optional.of(testWallet));
        Mockito.doNothing().when(timelineProducer).sendEvent(any(QueueOperationDTO.class));
        Mockito.doNothing().when(errorProducer).sendEvent(any());
        Mockito.when(timelineMapper.ackToTimeline(INSTRUMENT_ACK_DTO_REJECTED_INSTRUMENT)).thenReturn(TEST_OPERATION_DTO);

        walletService.processAck(INSTRUMENT_ACK_DTO_REJECTED_INSTRUMENT);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(0))
                .updateInstrumentNumber(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyInt(), Mockito.anyString());
        Mockito.verify(errorProducer, Mockito.times(0)).sendEvent(any());
    }

    @Test
    void processAck_queue_error() {
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        doThrow(new WalletUpdateException(""))
                .when(timelineProducer)
                .sendEvent(any(QueueOperationDTO.class));

        Mockito.doNothing().when(errorProducer).sendEvent(any());

        Mockito.when(timelineMapper.ackToTimeline(INSTRUMENT_ACK_DTO_ADD_INSTRUMENT)).thenReturn(TEST_OPERATION_DTO);

        walletService.processAck(INSTRUMENT_ACK_DTO_ADD_INSTRUMENT);

        Mockito.verify(errorProducer, Mockito.times(1)).sendEvent(any());
    }

    @Test
    void enrollInstrument_ko_initiative_ended() {
        // Given
        testWallet.setInitiativeEndDate(LocalDate.MIN);
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        // When
        InitiativeInvalidException exception = assertThrows(InitiativeInvalidException.class,
                () -> walletService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL_APP_IO));

        // Then
        assertEquals(INITIATIVE_ENDED, exception.getCode());
        assertEquals(String.format(INITIATIVE_ENDED_MSG, INITIATIVE_ID), exception.getMessage());

        verify(walletRepositoryMock, times(1)).findById(any());
        verifyNoMoreInteractions(walletRepositoryMock);
        verifyNoInteractions(paymentInstrumentRestConnector);
        verifyNoInteractions(timelineProducer);
    }

    @Test
    void enrollInstrument_not_found() {
        // Given
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.empty());

        // When
        UserNotOnboardedException exception = assertThrows(UserNotOnboardedException.class,
                () -> walletService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL_APP_IO));

        // Then
        assertEquals(USER_NOT_ONBOARDED, exception.getCode());
        assertEquals(String.format(USER_NOT_ONBOARDED_MSG, INITIATIVE_ID), exception.getMessage());

        verify(walletRepositoryMock, times(1)).findById(any());
        verifyNoMoreInteractions(walletRepositoryMock);
        verifyNoInteractions(paymentInstrumentRestConnector);
        verifyNoInteractions(timelineProducer);
    }

    @Test
    void enrollInstrument_unsubscribed() {
        // Given
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(TEST_WALLET_UNSUBSCRIBED));

        // When
        UserUnsubscribedException exception = assertThrows(UserUnsubscribedException.class,
                () -> walletService.enrollInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL_APP_IO));

        // Then
        assertEquals(USER_UNSUBSCRIBED, exception.getCode());
        assertEquals(String.format(ERROR_UNSUBSCRIBED_INITIATIVE_MSG, INITIATIVE_ID), exception.getMessage());

        verify(walletRepositoryMock, times(1)).findById(any());
        verifyNoMoreInteractions(walletRepositoryMock);
        verifyNoInteractions(paymentInstrumentRestConnector);
        verifyNoInteractions(timelineProducer);
    }

    @Test
    void deleteInstrument_ok() {
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        testWallet.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name());
        testWallet.setNInstr(1);
        testWallet.setIban(null);
        testWallet.setInitiativeEndDate(LocalDate.MAX);

        Mockito.doNothing()
                .when(paymentInstrumentRestConnector)
                .deleteInstrument(any(DeactivationBodyDTO.class));

        Mockito.doNothing().when(timelineProducer).sendEvent(any(QueueOperationDTO.class));

        walletService.deleteInstrument(INITIATIVE_ID, USER_ID, ID_WALLET, CHANNEL_APP_IO);

        assertEquals(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name(), testWallet.getStatus());
        assertEquals(1, testWallet.getNInstr());
    }

    @Test
    void deleteInstrument_ko_initiative_ended() {
        // Given
        testWallet.setInitiativeEndDate(LocalDate.MIN);
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        // When
        InitiativeInvalidException exception = assertThrows(InitiativeInvalidException.class,
                () -> walletService.deleteInstrument(INITIATIVE_ID, USER_ID, INSTRUMENT_ID, CHANNEL_APP_IO));

        // Then
        assertEquals(INITIATIVE_ENDED, exception.getCode());
        assertEquals(String.format(INITIATIVE_ENDED_MSG, INITIATIVE_ID), exception.getMessage());

        verify(walletRepositoryMock, times(1)).findById(any());
        verifyNoMoreInteractions(walletRepositoryMock);
        verifyNoInteractions(paymentInstrumentRestConnector);
        verifyNoInteractions(timelineMapper);
        verifyNoInteractions(timelineProducer);
    }

    @Test
    void deleteInstrument_ko_feignexception() {
        // Given
        testWallet.setInitiativeEndDate(LocalDate.MAX);
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        doThrow(new PaymentInstrumentNotFoundException(PAYMENT_INSTRUMENT_NOT_FOUND_MSG))
                .when(paymentInstrumentRestConnector)
                .deleteInstrument(any(DeactivationBodyDTO.class));

        // When
        PaymentInstrumentNotFoundException exception = assertThrows(PaymentInstrumentNotFoundException.class,
                () -> walletService.deleteInstrument(INITIATIVE_ID, USER_ID, INSTRUMENT_ID, CHANNEL_APP_IO));

        // Then
        assertEquals(PAYMENT_INSTRUMENT_NOT_FOUND, exception.getCode());
        assertEquals(PAYMENT_INSTRUMENT_NOT_FOUND_MSG, exception.getMessage());

        verify(walletRepositoryMock, times(1)).findById(any());
        verifyNoMoreInteractions(walletRepositoryMock);
        verify(paymentInstrumentRestConnector, times(1)).deleteInstrument(any());
        verify(timelineMapper, times(1)).ackToTimeline(any());
        verify(timelineProducer, times(1)).sendEvent(any());
    }

    @Test
    void deleteInstrument_not_found() {
        // Given
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.empty());

        // When
        UserNotOnboardedException exception = assertThrows(UserNotOnboardedException.class,
                () -> walletService.deleteInstrument(INITIATIVE_ID, USER_ID, INSTRUMENT_ID, CHANNEL_APP_IO));

        // Then
        assertEquals(USER_NOT_ONBOARDED, exception.getCode());
        assertEquals(String.format(USER_NOT_ONBOARDED_MSG, INITIATIVE_ID), exception.getMessage());

        verify(walletRepositoryMock, times(1)).findById(any());
        verifyNoMoreInteractions(walletRepositoryMock);
        verifyNoInteractions(paymentInstrumentRestConnector);
        verifyNoInteractions(timelineMapper);
        verifyNoInteractions(timelineProducer);
    }

    @Test
    void getEnrollmentStatus_ok() {
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        EnrollmentStatusDTO actual = walletService.getEnrollmentStatus(INITIATIVE_ID, USER_ID);
        assertEquals(testWallet.getStatus(), actual.getStatus());
    }

    @Test
    void getEnrollmentStatus_ko() {
        // Given
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.empty());


        // When
        UserNotOnboardedException exception = assertThrows(UserNotOnboardedException.class,
                () -> walletService.getEnrollmentStatus(INITIATIVE_ID, USER_ID));

        // Then
        assertEquals(USER_NOT_ONBOARDED, exception.getCode());
        assertEquals(String.format(USER_NOT_ONBOARDED_MSG, INITIATIVE_ID), exception.getMessage());

        assertEquals(USER_NOT_ONBOARDED, exception.getCode());
        assertEquals(String.format(USER_NOT_ONBOARDED_MSG, INITIATIVE_ID), exception.getMessage());

        verify(walletRepositoryMock, times(1)).findById(any());
        verifyNoMoreInteractions(walletRepositoryMock);
    }

    @Test
    void enrollIban_ok_only_iban() {
        testWallet.setStatus(WalletStatus.NOT_REFUNDABLE.name());
        testWallet.setNInstr(0);
        testWallet.setInitiativeEndDate(LocalDate.MAX);

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));


        Mockito.doAnswer(
                        invocationOnMock -> {
                            testWallet.setIban(IBAN_OK);
                            testWallet.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_IBAN.name());
                            return null;
                        })
                .when(walletUpdatesRepositoryMock)
                .enrollIban(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.eq(IBAN_OK), Mockito.anyString());

        walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, CHANNEL, DESCRIPTION_OK);

        assertEquals(WalletStatus.NOT_REFUNDABLE_ONLY_IBAN.name(), testWallet.getStatus());
    }

    @Test
    void enrollIban_ok_error_queue() {
        testWallet.setStatus(WalletStatus.NOT_REFUNDABLE.name());
        testWallet.setNInstr(0);
        testWallet.setIban(null);
        testWallet.setInitiativeEndDate(LocalDate.MAX);

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        Mockito.doAnswer(
                        invocationOnMock -> {
                            testWallet.setIban(IBAN_OK);
                            testWallet.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_IBAN.name());
                            return null;
                        })
                .when(walletUpdatesRepositoryMock)
                .enrollIban(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.eq(IBAN_OK), Mockito.anyString());

        doThrow(new WalletUpdateException(""))
                .when(ibanProducer)
                .sendIban(any(IbanQueueDTO.class));

        walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, CHANNEL, DESCRIPTION_OK);

        assertEquals(WalletStatus.NOT_REFUNDABLE_ONLY_IBAN.name(), testWallet.getStatus());
        Mockito.verify(errorProducer, Mockito.times(1)).sendEvent(any());
    }

    @Test
    void enrollIban_ok_with_instrument() {
        testWallet.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name());
        testWallet.setNInstr(1);
        testWallet.setInitiativeEndDate(LocalDate.MAX);

        Mockito.doNothing().when(ibanProducer).sendIban(any(IbanQueueDTO.class));

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));


        Mockito.doAnswer(
                        invocationOnMock -> {
                            testWallet.setIban(IBAN_OK);
                            testWallet.setStatus(WalletStatus.REFUNDABLE.name());
                            return null;
                        })
                .when(walletUpdatesRepositoryMock)
                .enrollIban(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.eq(IBAN_OK), Mockito.anyString());
        Mockito.doNothing().when(timelineProducer).sendEvent(any(QueueOperationDTO.class));

        walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, CHANNEL, DESCRIPTION_OK);

        assertEquals(INITIATIVE_ID, testWallet.getInitiativeId());
        assertEquals(USER_ID, testWallet.getUserId());

        assertEquals(WalletStatus.REFUNDABLE.name(), testWallet.getStatus());
    }

    @Test
    void enrollIban_ok_idemp() {
        Mockito.doNothing().when(ibanProducer).sendIban(any(IbanQueueDTO.class));
        testWallet.setIban(IBAN_OK);
        testWallet.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_IBAN.name());
        testWallet.setNInstr(0);
        testWallet.setInitiativeEndDate(LocalDate.MAX);

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));


        walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, CHANNEL, DESCRIPTION_OK);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(0))
                .enrollIban(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.eq(IBAN_OK), Mockito.anyString());
    }

    @Test
    void enrollIban_ko_iban_not_italian() {
        // Given
        testWallet.setIban(null);
        testWallet.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name());
        testWallet.setInitiativeEndDate(LocalDate.MAX);

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        // When
        InvalidIbanException exception = assertThrows(InvalidIbanException.class,
                () -> walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_KO_NOT_IT, CHANNEL, DESCRIPTION_OK));

        // Then
        assertEquals(IBAN_NOT_ITALIAN, exception.getCode());
        assertEquals(String.format(ERROR_IBAN_NOT_ITALIAN, IBAN_KO_NOT_IT), exception.getMessage());

        verifyNoInteractions(walletUpdatesRepositoryMock);
        verifyNoInteractions(ibanProducer);
        verifyNoInteractions(errorProducer);
    }

    @Test
    void enrollIban_ko_iban_wrong() {
        testWallet.setIban(null);
        testWallet.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name());
        testWallet.setInitiativeEndDate(LocalDate.MAX);

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        try {
            walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_WRONG, CHANNEL, DESCRIPTION_OK);
            Assertions.fail();
        } catch (IbanFormatException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    void enrollIban_ko_iban_digit_control() {
        testWallet.setIban(null);
        testWallet.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name());
        testWallet.setInitiativeEndDate(LocalDate.MAX);

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        try {
            walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_WRONG_DIGIT, CHANNEL, DESCRIPTION_OK);
            Assertions.fail();
        } catch (InvalidCheckDigitException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    void enrollIban_not_found() {
        // Given
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.empty());

        // When
        UserNotOnboardedException exception = assertThrows(UserNotOnboardedException.class,
                () -> walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, CHANNEL, DESCRIPTION_OK));

        // Then
        assertEquals(USER_NOT_ONBOARDED, exception.getCode());
        assertEquals(String.format(USER_NOT_ONBOARDED_MSG, INITIATIVE_ID), exception.getMessage());

        verifyNoInteractions(walletUpdatesRepositoryMock);
        verifyNoInteractions(ibanProducer);
        verifyNoInteractions(errorProducer);
    }

    @Test
    void enrollIban_ko_unsubscribe() {
        // Given
        testWallet.setInitiativeEndDate(LocalDate.MAX);

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(TEST_WALLET_UNSUBSCRIBED));

        // When
        UserUnsubscribedException exception = assertThrows(UserUnsubscribedException.class,
                () -> walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, CHANNEL, DESCRIPTION_OK));

        // Then
        assertEquals(USER_UNSUBSCRIBED, exception.getCode());
        assertEquals(String.format(ERROR_UNSUBSCRIBED_INITIATIVE_MSG, INITIATIVE_ID), exception.getMessage());

        verifyNoInteractions(walletUpdatesRepositoryMock);
        verifyNoInteractions(ibanProducer);
        verifyNoInteractions(errorProducer);
    }

    @Test
    void enrollIban_ko_initiative_ended() {
        // Given
        testWallet.setInitiativeEndDate(LocalDate.MIN);

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        // When
        InitiativeInvalidException exception = assertThrows(InitiativeInvalidException.class,
                () -> walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, CHANNEL, DESCRIPTION_OK));

        // Then
        assertEquals(INITIATIVE_ENDED, exception.getCode());
        assertEquals(String.format(INITIATIVE_ENDED_MSG, INITIATIVE_ID), exception.getMessage());

        verify(walletRepositoryMock, times(1)).findById(any());
        verifyNoMoreInteractions(walletRepositoryMock);
        verifyNoInteractions(ibanProducer);
        verifyNoInteractions(errorProducer);
    }

    @Test
    void enrollIban_update_ok() {
        Mockito.doNothing().when(ibanProducer).sendIban(any(IbanQueueDTO.class));
        testWallet.setIban(IBAN_OK_OTHER);
        testWallet.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_IBAN.name());
        testWallet.setNInstr(0);
        testWallet.setInitiativeEndDate(LocalDate.MAX);

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));


        Mockito.doAnswer(
                        invocationOnMock -> {
                            testWallet.setIban(IBAN_OK);
                            return null;
                        })
                .when(walletUpdatesRepositoryMock)
                .enrollIban(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.eq(IBAN_OK), Mockito.anyString());

        walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, CHANNEL, DESCRIPTION_OK);

        assertEquals(WalletStatus.NOT_REFUNDABLE_ONLY_IBAN.name(), testWallet.getStatus());
    }

    @Test
    void enrollIban_ko_discountInitiative() {
        // Given
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(TEST_WALLET_DISCOUNT));

        // When
        EnrollmentNotAllowedException exception = assertThrows(EnrollmentNotAllowedException.class,
                () -> walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, CHANNEL, DESCRIPTION_OK));

        // Then
        assertEquals(ENROLL_IBAN_DISCOUNT_INITIATIVE, exception.getCode());
        assertEquals(String.format(IBAN_ENROLL_NOT_ALLOWED_DISCOUNT_MSG, INITIATIVE_ID), exception.getMessage());

        verify(walletRepositoryMock, times(1)).findById(any());
        verifyNoMoreInteractions(walletRepositoryMock);
        verifyNoInteractions(paymentInstrumentRestConnector);
        verifyNoInteractions(onboardingRestConnector);
    }

    @Test
    void getWalletDetail_ok() {
        Mockito.when(walletRepositoryMock.findByIdAndUserId(ID_WALLET, USER_ID))
                .thenReturn(Optional.of(testWallet));
        testWallet.setIban(IBAN_OK);

        Mockito.when(walletMapper.toInitiativeDTO(any(Wallet.class))).thenReturn(WALLET_DTO);

        WalletDTO actual = walletService.getWalletDetail(INITIATIVE_ID, USER_ID);
        assertEquals(WALLET_DTO.getInitiativeId(), actual.getInitiativeId());
        assertEquals(WALLET_DTO.getInitiativeName(), actual.getInitiativeName());
        assertEquals(WALLET_DTO.getStatus(), actual.getStatus());
        assertEquals(WALLET_DTO.getInitiativeEndDate(), actual.getInitiativeEndDate());
        assertEquals(WALLET_DTO.getIban(), actual.getIban());
        assertEquals(WALLET_DTO.getNInstr(), actual.getNInstr());
        assertEquals(WALLET_DTO.getAmountCents(), actual.getAmountCents());
        assertEquals(WALLET_DTO.getAccruedCents(), actual.getAccruedCents());
        assertEquals(WALLET_DTO.getRefundedCents(), actual.getRefundedCents());
        assertEquals(WALLET_DTO.getNTrx(), actual.getNTrx());
        assertEquals(WALLET_DTO.getMaxTrx(), actual.getMaxTrx());

    }

    @Test
    void getWalletDetail_ko() {
        // Given
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.empty());

        // When
        UserNotOnboardedException exception = assertThrows(UserNotOnboardedException.class,
                () -> walletService.getWalletDetail(INITIATIVE_ID, USER_ID));

        // Then
        assertEquals(USER_NOT_ONBOARDED, exception.getCode());
        assertEquals(String.format(USER_NOT_ONBOARDED_MSG, INITIATIVE_ID), exception.getMessage());

        verify(walletRepositoryMock, times(1)).findByIdAndUserId(any(), any());
        verifyNoMoreInteractions(walletRepositoryMock);
    }

    @Test
    void getWalletDetail_issuer_ok() {
        Mockito.when(walletRepositoryMock.findByIdAndUserId(ID_WALLET, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET_ISSUER));

        Mockito.when(walletMapper.toIssuerInitiativeDTO(any(Wallet.class)))
                .thenReturn(WALLET_ISSUER_DTO);

        WalletDTO actual = walletService.getWalletDetailIssuer(INITIATIVE_ID, USER_ID);
        assertEquals(WALLET_DTO.getAmountCents(), actual.getAmountCents());
        assertEquals(WALLET_DTO.getAccruedCents(), actual.getAccruedCents());
        assertEquals(WALLET_DTO.getRefundedCents(), actual.getRefundedCents());

    }
    
    @Test
    void getInitiativeList_ok() {
        testWallet.setIban(IBAN_OK);
        List<Wallet> walletList = new ArrayList<>();
        walletList.add(testWallet);
        walletList.add(TEST_WALLET_DISCOUNT);

        Mockito.when(walletRepositoryMock.findByUserId(USER_ID)).thenReturn(walletList);
        Mockito.when(walletMapper.toInitiativeDTO(any(Wallet.class))).thenReturn(WALLET_DTO);

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
        Mockito.when(walletMapper.map(any())).thenReturn(testWallet);
        walletService.createWallet(EVALUATION_ONBOARDING_OK);
        Mockito.verify(walletRepositoryMock, Mockito.times(1)).save(any());
        Mockito.verify(timelineProducer, Mockito.times(1)).sendEvent(any());
    }

    @Test
    void createWalletFamily() {
        Wallet wallet = TEST_WALLET_FAMILY.toBuilder().userId(USER_ID.concat("_1")).build();
        Mockito.when(walletMapper.map(any())).thenReturn(wallet);
        Mockito.when(walletRepositoryMock.findByInitiativeIdAndFamilyId(INITIATIVE_ID, FAMILY_ID))
                .thenReturn(List.of(TEST_WALLET_FAMILY.toBuilder().amountCents(TEST_AMOUNT).build()));

        walletService.createWallet(EVALUATION_ONBOARDING_OK);

        Assertions.assertEquals(0, TEST_AMOUNT.compareTo(wallet.getAmountCents()));
        Mockito.verify(walletRepositoryMock).save(any(Wallet.class));
        Mockito.verify(timelineProducer).sendEvent(any());
    }

    @Test
    void createWalletJoined() {
        Mockito.when(walletMapper.map(any())).thenReturn(testWallet);
        walletService.createWallet(EVALUATION_JOINED);
        /**
         * Vedi commento per gestione Joined in metodo
         */
//        Mockito.verify(walletRepositoryMock, Mockito.times(1)).save(any());
//        Mockito.verify(timelineProducer, Mockito.times(1)).sendEvent(any());
        Mockito.verifyNoInteractions(walletRepositoryMock);
        Mockito.verifyNoInteractions(timelineProducer);
    }

    @Test
    void createWallet_doNothing() {
        walletService.createWallet(OUTCOME_KO);
        Mockito.verify(walletRepositoryMock, Mockito.times(0)).save(any());
        Mockito.verify(timelineProducer, Mockito.times(0)).sendEvent(any());
    }

    @Test
    void createWallet_initiativeDiscount() {
        Mockito.when(walletMapper.map(any())).thenReturn(testWallet);
        walletService.createWallet(OUTCOME_OK_DISCOUNT);
        Mockito.verify(walletRepositoryMock, Mockito.times(1)).save(any());
        Mockito.verify(timelineProducer, Mockito.times(1)).sendEvent(any());
        assertEquals(testWallet.getStatus(), WalletStatus.REFUNDABLE.name());
    }


    @Test
    void processIbanOutcome_error_queue() {
        testWallet.setIban(IBAN_OK);
        IbanQueueWalletDTO iban =
                new IbanQueueWalletDTO(
                        USER_ID, INITIATIVE_ID, IBAN_OK, STATUS_KO, LocalDateTime.now().toString(), CHANNEL);
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        doThrow(new WalletUpdateException(""))
                .when(notificationProducer)
                .sendNotification(any(NotificationQueueDTO.class));


        walletService.processIbanOutcome(iban);


        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(1))
                .deleteIban(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString());
        Mockito.verify(errorProducer, Mockito.times(1)).sendEvent(any());
    }

    @Test
    void processIbanOutcome_statusKO_ok() {
        testWallet.setIban(IBAN_OK);
        IbanQueueWalletDTO iban =
                new IbanQueueWalletDTO(
                        USER_ID, INITIATIVE_ID, IBAN_OK, STATUS_KO, LocalDateTime.now().toString(), CHANNEL);
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        Mockito.doNothing()
                .when(notificationProducer)
                .sendNotification(any(NotificationQueueDTO.class));

        walletService.processIbanOutcome(iban);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(1))
                .deleteIban(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString());
    }

    @Test
    void processIbanOutcome_not_found() {
        IbanQueueWalletDTO iban =
                new IbanQueueWalletDTO(
                        USER_ID, INITIATIVE_ID, IBAN_OK, STATUS_KO, LocalDateTime.now().toString(), CHANNEL);
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.empty());

        walletService.processIbanOutcome(iban);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(0))
                .deleteIban(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString());
    }

    @Test
    void processIbanOutcome_duplicate() {
        IbanQueueWalletDTO iban =
                new IbanQueueWalletDTO(
                        USER_ID, INITIATIVE_ID, IBAN_OK_OTHER, STATUS_KO, LocalDateTime.now().toString(), CHANNEL);
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));


        walletService.processIbanOutcome(iban);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(0))
                .deleteIban(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString());
    }

    @Test
    void processIbanOutcome_status_ok() {
        IbanQueueWalletDTO iban =
                new IbanQueueWalletDTO(
                        USER_ID, INITIATIVE_ID, IBAN_OK, "OK", LocalDateTime.now().toString(), CHANNEL);

        walletService.processIbanOutcome(iban);

        Mockito.verify(timelineMapper, Mockito.times(1))
                .ibanToTimeline(INITIATIVE_ID, USER_ID, IBAN_OK, CHANNEL);
        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(0))
                .deleteIban(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString());
    }

    @Test
    void unsubscribe_ok() {

        testWallet.setStatus(WalletStatus.NOT_REFUNDABLE.name());

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        Mockito.doAnswer(
                        invocationOnMock -> {
                            testWallet.setRequestUnsubscribeDate(LocalDateTime.now());
                            testWallet.setStatus(WalletStatus.UNSUBSCRIBED);
                            return null;
                        })
                .when(walletRepositoryMock)
                .save(any(Wallet.class));

        walletService.unsubscribe(INITIATIVE_ID, USER_ID, CHANNEL_APP_IO);
        assertNotNull(testWallet.getRequestUnsubscribeDate());
        assertEquals(WalletStatus.UNSUBSCRIBED, testWallet.getStatus());

    }

    @Test
    void unsubscribe_ko_unsubscribed() {

        testWallet.setStatus(WalletStatus.UNSUBSCRIBED);

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));


        walletService.unsubscribe(INITIATIVE_ID, USER_ID, CHANNEL_APP_IO);


        Mockito.verify(walletRepositoryMock, Mockito.times(0)).save(any());
    }

    @Test
    void unsubscribe_ko() {
        // Given
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.empty());

        // When
        UserNotOnboardedException exception = assertThrows(UserNotOnboardedException.class,
                () -> walletService.unsubscribe(INITIATIVE_ID, USER_ID, CHANNEL_APP_IO));

        // Then
        assertEquals(USER_NOT_ONBOARDED, exception.getCode());
        assertEquals(String.format(USER_NOT_ONBOARDED_MSG, INITIATIVE_ID), exception.getMessage());

        verifyNoInteractions(paymentInstrumentRestConnector);
        verifyNoInteractions(onboardingRestConnector);
    }

    @SneakyThrows
    @Test
    void processTransaction_ok_counter_valid() {
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        Mockito.when(
                        walletUpdatesRepositoryMock.rewardTransaction(
                                Mockito.eq(INITIATIVE_ID),
                                Mockito.eq(USER_ID),
                                any(),
                                any(),
                                any(),
                                any())
                )
                .thenReturn(testWallet);

        walletService.processTransaction(MessageBuilder.withPayload(
                objectMapper.writeValueAsString(REWARD_TRX_DTO_REWARDED)).build());

        Mockito.verify(timelineProducer, Mockito.times(1)).sendEvent(any());
    }

    @SneakyThrows
    @Test
    void processTransaction_ko_counter_not_valid() {

        testWallet.setCounterVersion(REWARD_TRX_DTO_REWARDED.getRewards().get(INITIATIVE_ID).getCounters().getVersion());

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        walletService.processTransaction(MessageBuilder.withPayload(
                objectMapper.writeValueAsString(REWARD_TRX_DTO_REWARDED)).build());

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(0))
                .rewardTransaction(
                    Mockito.eq(INITIATIVE_ID),
                    Mockito.eq(USER_ID),
                    any(),
                    any(),
                    any(),
                    any()
                );

        Mockito.verify(timelineProducer, Mockito.times(1)).sendEvent(any());
    }

    @SneakyThrows
    @Test
    void processTransaction_sync_ok() {
        Mockito.when(
                        walletUpdatesRepositoryMock.rewardTransaction(
                                Mockito.eq(INITIATIVE_ID),
                                Mockito.eq(USER_ID),
                                any(),
                                any(),
                                any(),
                                any())
                )
                .thenReturn(testWallet);
        walletService.processTransaction(MessageBuilder.withPayload(objectMapper.writeValueAsString(
                REWARD_TRX_DTO_SYNC_REWARDED)).build());
        Mockito.verify(timelineProducer, Mockito.times(1)).sendEvent(any());
    }

    @SneakyThrows
    @Test
    void processTransaction_captured_reward_empty() {
        Mockito.when(
                        walletUpdatesRepositoryMock.rewardTransaction(
                                Mockito.eq(INITIATIVE_ID),
                                Mockito.eq(USER_ID),
                                any(),
                                any(),
                                any(),
                                any())
                )
                .thenReturn(testWallet);
        walletService.processTransaction(MessageBuilder.withPayload(objectMapper.writeValueAsString(
                REWARD_TRX_DTO_SYNC_CAPTURED_NOREWARDS)).build());
    }

    @SneakyThrows
    @Test
    void processTransaction_captured_reward_populated() {
        Mockito.when(
                        walletUpdatesRepositoryMock.rewardTransaction(
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                any())
                )
                .thenReturn(testWallet);
        walletService.processTransaction(MessageBuilder.withPayload(objectMapper
                .writeValueAsString(REWARD_TRX_DTO_SYNC_CAPTURED_REWARDS)).build());
    }

    @SneakyThrows
    @Test
    void processTransaction_captured_reward_populated_with_wallet() {
        Mockito.when(
                        walletUpdatesRepositoryMock.rewardTransaction(
                                any(),
                                any(),
                                any(),
                                any(),
                                any(),
                                any())
                )
                .thenReturn(testWallet);
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));
        walletService.processTransaction(MessageBuilder.withPayload(
                objectMapper.writeValueAsString(REWARD_TRX_DTO_SYNC_CAPTURED_REWARDS)).build());
        verify(walletRepositoryMock).save(any());
    }

    @SneakyThrows
    @Test
    void processTransaction_sync_ok_auth() {
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        Mockito.when(
                        walletUpdatesRepositoryMock.rewardTransaction(
                                Mockito.eq(INITIATIVE_ID),
                                Mockito.eq(USER_ID),
                                any(),
                                any(),
                                any(),
                                any())
        )
                .thenReturn(testWallet);

        walletService.processTransaction(MessageBuilder.withPayload(objectMapper.writeValueAsString(
                REWARD_TRX_DTO_SYNC_AUTHORIZED)).build());

        Mockito.verify(timelineProducer, Mockito.times(1)).sendEvent(any());
    }

    @SneakyThrows
    @Test
    void processTransaction_family_both_counters_valid() {
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(TEST_WALLET_FAMILY));

        Mockito.when(walletUpdatesRepositoryMock.rewardFamilyUserTransaction(
                        Mockito.eq(INITIATIVE_ID),
                        Mockito.eq(USER_ID),
                        any(),
                        any(),
                        any())
                )
                .thenReturn(TEST_WALLET_FAMILY);

        Mockito.when(walletUpdatesRepositoryMock.rewardFamilyTransaction(
                        Mockito.eq(INITIATIVE_ID),
                        Mockito.eq(FAMILY_ID),
                        any(),
                        any(),
                        any())
                )
                .thenReturn(true);

        walletService.processTransaction(MessageBuilder.withPayload(
                objectMapper.writeValueAsString(REWARD_TRX_DTO_REWARDED)).build());

        Mockito.verify(timelineProducer).sendEvent(any());
    }

    @SneakyThrows
    @Test
    void processTransaction_family_old_counter_not_in_history() {

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(TEST_WALLET_FAMILY_OLD_COUNTER_NOT_IN_HISTORY));


        Mockito.when(walletUpdatesRepositoryMock.rewardFamilyUserTransaction(
                        Mockito.eq(INITIATIVE_ID),
                        Mockito.eq(USER_ID),
                        any(),
                        any(),
                        any())
                )
                .thenReturn(TEST_WALLET_FAMILY);

        walletService.processTransaction(MessageBuilder.withPayload(
                objectMapper.writeValueAsString(REWARD_TRX_DTO_REWARDED)).build());

        Mockito.verify(walletUpdatesRepositoryMock,Mockito.times(0))
                .rewardFamilyTransaction(
                        Mockito.eq(INITIATIVE_ID),
                        Mockito.eq(USER_ID),
                        any(),
                        any(),
                        any()
                );


        Mockito.verify(timelineProducer).sendEvent(any());
    }

    @SneakyThrows
    @Test
    void processTransaction_family_counters_not_valid() {

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(TEST_WALLET_FAMILY_COUNTERS_NOT_VALID));

        walletService.processTransaction(MessageBuilder.withPayload(
                objectMapper.writeValueAsString(REWARD_TRX_DTO_REWARDED)).build());

        Mockito.verify(walletUpdatesRepositoryMock,Mockito.times(0))
                .rewardFamilyTransaction(
                        Mockito.eq(INITIATIVE_ID),
                        Mockito.eq(FAMILY_ID),
                        any(),
                        any(),
                        any()
                );

        Mockito.verify(walletUpdatesRepositoryMock,Mockito.times(0))
                .rewardFamilyUserTransaction(
                        Mockito.eq(INITIATIVE_ID),
                        Mockito.eq(USER_ID),
                        any(),
                        any(),
                        any()
                );


        Mockito.verify(timelineProducer).sendEvent(any());
    }


    @SneakyThrows
    @Test
    void processTransaction_family_ko() {
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(TEST_WALLET_FAMILY));

        Mockito.when(walletUpdatesRepositoryMock.rewardFamilyUserTransaction(
                        Mockito.eq(INITIATIVE_ID),
                        Mockito.eq(USER_ID),
                        any(),
                        any(),
                        any())
                )
                .thenReturn(TEST_WALLET_FAMILY);

        Mockito.when(walletUpdatesRepositoryMock.rewardFamilyTransaction(
                        Mockito.eq(INITIATIVE_ID),
                        Mockito.eq(FAMILY_ID),
                        any(),
                        any(),
                        any())
                )
                .thenReturn(false);

        walletService.processTransaction(MessageBuilder.withPayload(
                objectMapper.writeValueAsString(REWARD_TRX_DTO_REWARDED)).build());

        Mockito.verify(timelineProducer, Mockito.never()).sendEvent(any());
        Mockito.verify(errorProducer).sendEvent(any());
    }

    @SneakyThrows
    @Test
    void processTransaction_ko() {
        Mockito.when(
                        walletUpdatesRepositoryMock.rewardTransaction(
                                Mockito.eq(INITIATIVE_ID),
                                Mockito.eq(USER_ID),
                                any(),
                                any(),
                                any(),
                                any())
        )
                .thenReturn(null);
        walletService.processTransaction(MessageBuilder.withPayload(
                objectMapper.writeValueAsString(REWARD_TRX_DTO_REWARDED)).build());
        Mockito.verify(timelineProducer, Mockito.times(0)).sendEvent(any());
    }

    @Test
    void unsubscribe_rollback_payment_instrument() {
        // Given
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        doThrow(new OnboardingInvocationException(ERROR_ONBOARDING_INVOCATION_MSG))
                .when(onboardingRestConnector)
                .disableOnboarding(any(UnsubscribeCallDTO.class));

        // When
        OnboardingInvocationException exception = assertThrows(OnboardingInvocationException.class,
                () -> walletService.unsubscribe(INITIATIVE_ID, USER_ID, CHANNEL_APP_IO));

        // Then
        assertEquals(GENERIC_ERROR, exception.getCode());
        assertEquals(ERROR_ONBOARDING_INVOCATION_MSG, exception.getMessage());

        verify(walletRepositoryMock, times(1)).findById(any());
        verifyNoMoreInteractions(walletRepositoryMock);
        verify(paymentInstrumentRestConnector, times(1)).disableAllInstrument(any());
        verify(paymentInstrumentRestConnector, times(1)).rollback(INITIATIVE_ID, USER_ID);
        verifyNoMoreInteractions(paymentInstrumentRestConnector);
    }

    @Test
    void unsubscribe_payment_instrument_ko() {
        // Given
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(TEST_WALLET_2));

        doThrow(new PaymentInstrumentInvocationException(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG))
                .when(paymentInstrumentRestConnector)
                .disableAllInstrument(any(UnsubscribeCallDTO.class));

        // When
        PaymentInstrumentInvocationException exception = assertThrows(PaymentInstrumentInvocationException.class,
                () -> walletService.unsubscribe(INITIATIVE_ID, USER_ID, CHANNEL_APP_IO));

        // Then
        assertEquals(GENERIC_ERROR, exception.getCode());
        assertEquals(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG, exception.getMessage());

        verify(walletRepositoryMock, times(1)).findById(any());
        verifyNoMoreInteractions(walletRepositoryMock);
        verify(paymentInstrumentRestConnector, times(1)).disableAllInstrument(any());
        verifyNoInteractions(onboardingRestConnector);
        verifyNoMoreInteractions(paymentInstrumentRestConnector);
    }

    @Test
    void unsubscribe_wallet_save_exception() {
        // Given
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(TEST_WALLET_2));
        when(walletRepositoryMock.save(any()))
                .thenThrow(new MongoClientException("ERROR"))
                .thenReturn(null);

        // When
        MongoClientException exception = assertThrows(MongoClientException.class,
                () -> walletService.unsubscribe(INITIATIVE_ID, USER_ID, CHANNEL_APP_IO));

        // Then
        assertEquals("ERROR", exception.getMessage());

        verify(walletRepositoryMock, times(1)).findById(any());
        verify(paymentInstrumentRestConnector, times(1)).disableAllInstrument(any());
        verify(onboardingRestConnector, times(1)).disableOnboarding(any());
        verify(walletRepositoryMock, times(2)).save(any());
        verify(onboardingRestConnector, times(1)).rollback(any(), any());
        verify(paymentInstrumentRestConnector, times(1)).rollback(any(), any());
    }

    @SneakyThrows
    @Test
    void processTransaction_not_rewarded() {
        walletService.processTransaction(MessageBuilder.withPayload(
                objectMapper.writeValueAsString(REWARD_TRX_DTO)).build());
        Mockito.verify(walletRepositoryMock, Mockito.times(0)).save(any());
        Mockito.verify(timelineProducer, Mockito.times(0)).sendEvent(any());
    }

    @SneakyThrows
    @Test
    void processTransaction_sync_not_authorized() {
        walletService.processTransaction(MessageBuilder.withPayload(objectMapper.writeValueAsString(
                REWARD_TRX_DTO_SYNC_NOT_AUTH)).build());
        Mockito.verify(walletRepositoryMock, Mockito.times(0)).save(any());
        Mockito.verify(timelineProducer, Mockito.times(0)).sendEvent(any());
    }

    @SneakyThrows
    @Test
    void processTransaction_not_sync_authorized() {
        walletService.processTransaction(MessageBuilder.withPayload(
                objectMapper.writeValueAsString(REWARD_TRX_DTO_AUTH)).build());
        Mockito.verify(walletRepositoryMock, Mockito.times(0)).save(any());
        Mockito.verify(timelineProducer, Mockito.times(0)).sendEvent(any());
    }

    @Test
    void update_wallet_ok() {
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        testWallet.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name());
        testWallet.setNInstr(1);
        testWallet.setIban(null);

        Mockito.doNothing().when(timelineProducer).sendEvent(any(QueueOperationDTO.class));
        Mockito.doAnswer(
                        invocationOnMock -> {
                            testWallet.setNInstr(0);
                            testWallet.setStatus(WalletStatus.NOT_REFUNDABLE.name());
                            return null;
                        })
                .when(walletUpdatesRepositoryMock)
                .decreaseInstrumentNumber(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString());


        List<WalletPIDTO> walletPIDTOList = new ArrayList<>();
        walletPIDTOList.add(new WalletPIDTO(INITIATIVE_ID, USER_ID, MASKED_PAN, BRAND_LOGO, BRAND_LOGO));
        WalletPIBodyDTO walletPIBodyDTO = new WalletPIBodyDTO(walletPIDTOList);
        walletService.updateWallet(walletPIBodyDTO);

        assertEquals(0, testWallet.getNInstr());
    }

    @Test
    void update_wallet_empty() {
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.empty());

        Mockito.doNothing().when(timelineProducer).sendEvent(any(QueueOperationDTO.class));

        List<WalletPIDTO> walletPIDTOList = new ArrayList<>();
        walletPIDTOList.add(new WalletPIDTO(INITIATIVE_ID, USER_ID, MASKED_PAN, BRAND_LOGO, BRAND_LOGO));
        WalletPIBodyDTO walletPIBodyDTO = new WalletPIBodyDTO(walletPIDTOList);
        walletService.updateWallet(walletPIBodyDTO);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(0))
                .decreaseInstrumentNumber(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString());
    }

    @Test
    void update_wallet_ok_queue_error() {
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        Mockito.doAnswer(
                        invocationOnMock -> {
                            testWallet.setNInstr(0);
                            testWallet.setStatus(WalletStatus.NOT_REFUNDABLE.name());
                            return null;
                        })
                .when(walletUpdatesRepositoryMock)
                .decreaseInstrumentNumber(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString());

        Mockito.when(
                        timelineMapper.deleteInstrumentToTimeline(
                                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(TEST_OPERATION_DTO);

        doThrow(new WalletUpdateException(""))
                .when(timelineProducer)
                .sendEvent(any(QueueOperationDTO.class));

        Mockito.doNothing().when(errorProducer).sendEvent(any());

        testWallet.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name());
        testWallet.setNInstr(1);
        testWallet.setIban(null);

        List<WalletPIDTO> walletPIDTOList = new ArrayList<>();
        walletPIDTOList.add(new WalletPIDTO(INITIATIVE_ID, USER_ID, MASKED_PAN, BRAND_LOGO, BRAND_LOGO));
        WalletPIBodyDTO walletPIBodyDTO = new WalletPIBodyDTO(walletPIDTOList);
        walletService.updateWallet(walletPIBodyDTO);

        assertEquals(0, testWallet.getNInstr());
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
                        BeneficiaryType.CITIZEN,
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

        testWallet.setRefundHistory(null);
        testWallet.setRefundedCents(TEST_REFUNDED);

        Mockito.when(timelineMapper.refundToTimeline(dto)).thenReturn(TEST_OPERATION_DTO);

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        walletService.processRefund(dto);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(1))
                .processRefund(
                        Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), any(), any());
        Mockito.verify(timelineProducer, Mockito.times(1)).sendEvent(TEST_OPERATION_DTO);
        Mockito.verify(errorProducer, Mockito.times(0)).sendEvent(any());
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
                        BeneficiaryType.CITIZEN,
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

        testWallet.setRefundHistory(null);
        testWallet.setRefundedCents(TEST_REFUNDED);

        Mockito.when(timelineMapper.refundToTimeline(dto)).thenReturn(TEST_OPERATION_DTO);

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        doThrow(new WalletUpdateException(""))
                .when(timelineProducer)
                .sendEvent(any(QueueOperationDTO.class));

        walletService.processRefund(dto);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(1))
                .processRefund(
                        Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), any(), any());
        Mockito.verify(errorProducer, Mockito.times(1)).sendEvent(any());
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
                        BeneficiaryType.CITIZEN,
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

        testWallet.setRefundHistory(map);
        testWallet.setRefundedCents(TEST_ACCRUED);

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        Mockito.when(timelineMapper.refundToTimeline(dto)).thenReturn(TEST_OPERATION_DTO);

        walletService.processRefund(dto);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(1))
                .processRefund(
                        Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), any(), any());
        Mockito.verify(timelineProducer, Mockito.times(1)).sendEvent(TEST_OPERATION_DTO);
        Mockito.verify(errorProducer, Mockito.times(0)).sendEvent(any());
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
                        BeneficiaryType.CITIZEN,
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

        testWallet.setRefundHistory(map);
        testWallet.setRefundedCents(TEST_ACCRUED);

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        walletService.processRefund(dto);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(0))
                .processRefund(
                        Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), any(), any());
        Mockito.verify(timelineProducer, Mockito.times(0))
                .sendEvent(any(QueueOperationDTO.class));
        Mockito.verify(errorProducer, Mockito.times(0)).sendEvent(any());
    }

    @Test
    void processRefund_merchantBeneficiaryTypeSkipped() {
        final RefundDTO dto =
                new RefundDTO(
                        "ID",
                        "EXT_ID",
                        "NOT_ID",
                        INITIATIVE_ID,
                        USER_ID,
                        BeneficiaryType.MERCHANT,
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

        walletService.processRefund(dto);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(0))
                .processRefund(
                        Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), any(), any());
        Mockito.verify(timelineProducer, Mockito.times(0))
                .sendEvent(any(QueueOperationDTO.class));
        Mockito.verify(errorProducer, Mockito.times(0)).sendEvent(any());
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
                        BeneficiaryType.CITIZEN,
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

        testWallet.setRefundHistory(null);
        testWallet.setRefundedCents(0L);

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        walletService.processRefund(dto);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(1))
                .processRefund(
                        Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), any(), any());
        Mockito.verify(timelineProducer, Mockito.times(0))
                .sendEvent(any(QueueOperationDTO.class));
        Mockito.verify(errorProducer, Mockito.times(0)).sendEvent(any());
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
                        BeneficiaryType.CITIZEN,
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

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.empty());

        walletService.processRefund(dto);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(0))
                .processRefund(
                        Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), any(), any());
        Mockito.verify(timelineProducer, Mockito.times(0))
                .sendEvent(any(QueueOperationDTO.class));
        Mockito.verify(errorProducer, Mockito.times(0)).sendEvent(any());
    }

    @Test
    void enrollInstrumentIssuer_ok() {

        final InstrumentIssuerDTO instrument =
                new InstrumentIssuerDTO("hpan", CHANNEL, "VISA", "VISA", "***");

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        testWallet.setIban(null);
        testWallet.setStatus(WalletStatus.NOT_REFUNDABLE.name());
        testWallet.setNInstr(0);
        testWallet.setInitiativeEndDate(LocalDate.MAX);

        Mockito.doNothing()
                .when(paymentInstrumentRestConnector)
                .enrollInstrumentIssuer(any(InstrumentIssuerCallDTO.class));


        walletService.enrollInstrumentIssuer(INITIATIVE_ID, USER_ID, instrument);

        assertEquals(WalletStatus.NOT_REFUNDABLE.name(), testWallet.getStatus());
        assertEquals(0, testWallet.getNInstr());
    }

    @Test
    void enrollInstrumentIssuer_discountInitiative() {
        // Given
        final InstrumentIssuerDTO instrument =
                new InstrumentIssuerDTO("hpan", CHANNEL, "VISA", "VISA", "***");

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(TEST_WALLET_DISCOUNT));

        // When
        EnrollmentNotAllowedException exception = assertThrows(EnrollmentNotAllowedException.class,
                () -> walletService.enrollInstrumentIssuer(INITIATIVE_ID, USER_ID, instrument));

        // Then
        assertEquals(ENROLL_INSTRUMENT_DISCOUNT_INITIATIVE, exception.getCode());
        assertEquals(String.format(PAYMENT_INSTRUMENT_ENROLL_NOT_ALLOWED_DISCOUNT_MSG, INITIATIVE_ID), exception.getMessage());

        verify(walletRepositoryMock, times(1)).findById(any());
        verifyNoMoreInteractions(walletRepositoryMock);
        verifyNoInteractions(paymentInstrumentRestConnector);
        verifyNoInteractions(timelineMapper);
        verifyNoInteractions(timelineProducer);
    }

    @Test
    void enrollInstrumentIssuer_unsubscribed() {
        // Given
        final InstrumentIssuerDTO instrument =
                new InstrumentIssuerDTO("hpan", CHANNEL, "VISA", "VISA", "***");

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(TEST_WALLET_UNSUBSCRIBED));

        // When
        UserUnsubscribedException exception = assertThrows(UserUnsubscribedException.class,
                () -> walletService.enrollInstrumentIssuer(INITIATIVE_ID, USER_ID, instrument));

        // Then
        assertEquals(USER_UNSUBSCRIBED, exception.getCode());
        assertEquals(String.format(ERROR_UNSUBSCRIBED_INITIATIVE_MSG, INITIATIVE_ID), exception.getMessage());

        verify(walletRepositoryMock, times(1)).findById(any());
        verifyNoMoreInteractions(walletRepositoryMock);
        verifyNoInteractions(paymentInstrumentRestConnector);
        verifyNoInteractions(timelineMapper);
        verifyNoInteractions(timelineProducer);
    }

    @Test
    void enrollInstrumentIssuer_ko_exception() {
        // Given
        testWallet.setInitiativeEndDate(LocalDate.MAX);

        final InstrumentIssuerDTO instrument =
                new InstrumentIssuerDTO("hpan", CHANNEL, "VISA", "VISA", "***");

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(TEST_WALLET_REFUNDABLE));

        Mockito.when(timelineMapper.ackToTimeline(INSTRUMENT_ACK_DTO_ADD_INSTRUMENT)).thenReturn(TEST_OPERATION_DTO);

        Mockito.doThrow(new PaymentInstrumentInvocationException(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG))
                .when(paymentInstrumentRestConnector)
                .enrollInstrumentIssuer(any(InstrumentIssuerCallDTO.class));

        // When
        PaymentInstrumentInvocationException exception = assertThrows(PaymentInstrumentInvocationException.class,
                () -> walletService.enrollInstrumentIssuer(INITIATIVE_ID, USER_ID, instrument));

        // Then
        assertEquals(GENERIC_ERROR, exception.getCode());
        assertEquals(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG, exception.getMessage());

        verify(walletRepositoryMock, times(1)).findById(any());
        verifyNoMoreInteractions(walletRepositoryMock);
        verify(paymentInstrumentRestConnector, times(1)).enrollInstrumentIssuer(any());
        verify(timelineMapper, times(1)).ackToTimeline(any());
        verify(timelineProducer, times(1)).sendEvent(any());
    }

    @Test
    void getInitiativesWithInstrument_ok() {
        List<Wallet> walletList = new ArrayList<>();
        walletList.add(testWallet);
        walletList.add(TEST_WALLET_REFUNDABLE);
        walletList.add(TEST_WALLET_UNSUBSCRIBED);

        Mockito.when(walletRepositoryMock.findByUserId(USER_ID)).thenReturn(walletList);
        Mockito.when(walletMapper.toInitiativeDTO(testWallet)).thenReturn(WALLET_DTO);
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
        instrStatusOnInitiative.add(new InitiativesStatusDTO(testWallet.getInitiativeId(),
                testWallet.getInitiativeName(), INSTRUMENT_ID, STATUS_ACTIVE));
        instrStatusOnInitiative.add(new InitiativesStatusDTO(TEST_WALLET_REFUNDABLE.getInitiativeId(),
                TEST_WALLET_REFUNDABLE.getInitiativeName(), INSTRUMENT_ID, STATUS_PENDING_ENROLLMENT));
        InitiativesWithInstrumentDTO initiativesWithInstrumentDTO =
                new InitiativesWithInstrumentDTO(ID_WALLET, MASKED_PAN, BRAND, instrStatusOnInitiative);

        Mockito.when(walletMapper.toInstrumentOnInitiativesDTO(Mockito.anyString(), any(), Mockito.anyList()))
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
        Mockito.when(walletMapper.toInstrumentOnInitiativesDTO(Mockito.anyString(), any(), Mockito.anyList()))
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
        walletList.add(testWallet);
        walletList.add(TEST_WALLET_REFUNDABLE);
        walletList.add(TEST_WALLET_UNSUBSCRIBED);

        WalletDTO walletDtoRef = new WalletDTO(FAMILY_ID, INITIATIVE_ID_REFUNDABLE, INITIATIVE_NAME, WalletStatus.REFUNDABLE.name(), VoucherStatus.ACTIVE.name(),
                IBAN_OK, TEST_DATE_ONLY_DATE.minusDays(1), TEST_DATE_ONLY_DATE.minusDays(3), TEST_DATE_ONLY_DATE.minusDays(2), 0, TEST_AMOUNT, TEST_AMOUNT, TEST_ACCRUED, TEST_REFUNDED,
                TEST_DATE, WalletConstants.INITIATIVE_REWARD_TYPE_REFUND, LOGO_URL, ORGANIZATION_NAME, 0L, 100L,
                0L,
                List.of(),
                SERVICE_ID,
                USERMAIL,
                Channel.WEB,
                NAME,
                SURNAME);
        WalletDTO walletDtoUnsub = new WalletDTO(FAMILY_ID, INITIATIVE_ID_REFUNDABLE, INITIATIVE_NAME, WalletStatus.REFUNDABLE.name(),
                VoucherStatus.ACTIVE.name(), IBAN_OK, TEST_DATE_ONLY_DATE.minusDays(1), TEST_DATE_ONLY_DATE.minusDays(3), TEST_DATE_ONLY_DATE.minusDays(2),
                0, TEST_AMOUNT,TEST_AMOUNT, TEST_ACCRUED, TEST_REFUNDED,
                TEST_DATE, WalletConstants.INITIATIVE_REWARD_TYPE_REFUND, LOGO_URL, ORGANIZATION_NAME, 0L, 100L,
                0L,
                List.of(),
                SERVICE_ID,
                USERMAIL,
                Channel.WEB,
                NAME,
                SURNAME);

        Mockito.when(walletRepositoryMock.findByUserId(USER_ID)).thenReturn(walletList);
        Mockito.when(walletMapper.toInitiativeDTO(testWallet)).thenReturn(WALLET_DTO);
        Mockito.when(walletMapper.toInitiativeDTO(TEST_WALLET_REFUNDABLE)).thenReturn(walletDtoRef);
        Mockito.when(walletMapper.toInitiativeDTO(TEST_WALLET_UNSUBSCRIBED)).thenReturn(walletDtoUnsub);

        InitiativesStatusDTO dtoTestWallet = new InitiativesStatusDTO(WALLET_DTO.getInitiativeId(),
                WALLET_DTO.getInitiativeName(), INSTRUMENT_ID, WalletConstants.INSTRUMENT_STATUS_DEFAULT);
        Mockito.when(walletMapper.toInstrStatusOnInitiativeDTO(WALLET_DTO)).thenReturn(dtoTestWallet);

        InstrumentDetailDTO instrDetailDTO = new InstrumentDetailDTO(MASKED_PAN, BRAND, new ArrayList<>());
        Mockito.when(paymentInstrumentRestConnector.getInstrumentInitiativesDetail(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyList())).thenReturn(instrDetailDTO);

        List<InitiativesStatusDTO> instrStatusOnInitiative = new ArrayList<>();
        instrStatusOnInitiative.add(new InitiativesStatusDTO(testWallet.getInitiativeId(),
                testWallet.getInitiativeName(), INSTRUMENT_ID, WalletConstants.INSTRUMENT_STATUS_DEFAULT));
        InitiativesWithInstrumentDTO initiativesWithInstrumentDTO =
                new InitiativesWithInstrumentDTO(ID_WALLET, MASKED_PAN, BRAND, instrStatusOnInitiative);

        Mockito.when(walletMapper.toInstrumentOnInitiativesDTO(Mockito.anyString(), any(), Mockito.anyList()))
                .thenReturn(initiativesWithInstrumentDTO);

        InitiativesWithInstrumentDTO result = walletService.getInitiativesWithInstrument(ID_WALLET, USER_ID);
        assertEquals(result.getIdWallet(), initiativesWithInstrumentDTO.getIdWallet());
        assertEquals(result.getMaskedPan(), initiativesWithInstrumentDTO.getMaskedPan());
        assertEquals(result.getBrand(), initiativesWithInstrumentDTO.getBrand());
        assertIterableEquals(result.getInitiativeList(), initiativesWithInstrumentDTO.getInitiativeList());
    }

    @Test
    void getInitiativesWithInstrument_payment_instrument_exception() {
        // Given
        List<Wallet> walletList = new ArrayList<>();
        walletList.add(testWallet);
        walletList.add(TEST_WALLET_REFUNDABLE);

        Mockito.when(walletRepositoryMock.findByUserId(USER_ID)).thenReturn(walletList);
        Mockito.when(walletMapper.toInitiativeDTO(testWallet)).thenReturn(WALLET_DTO);
        Mockito.when(walletMapper.toInitiativeDTO(TEST_WALLET_REFUNDABLE)).thenReturn(WALLET_REFUNDABLE_DTO);

        doThrow(new PaymentInstrumentInvocationException(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG))
                .when(paymentInstrumentRestConnector).getInstrumentInitiativesDetail(ID_WALLET, USER_ID, WalletConstants.FILTER_INSTRUMENT_STATUS_LIST);

        // When
        PaymentInstrumentInvocationException exception = assertThrows(PaymentInstrumentInvocationException.class,
                () -> walletService.getInitiativesWithInstrument(ID_WALLET, USER_ID));

        // Then
        assertEquals(GENERIC_ERROR, exception.getCode());
        assertEquals(ERROR_PAYMENT_INSTRUMENT_INVOCATION_MSG, exception.getMessage());
        verify(paymentInstrumentRestConnector, times(1))
                .getInstrumentInitiativesDetail(ID_WALLET, USER_ID, WalletConstants.FILTER_INSTRUMENT_STATUS_LIST);
        verifyNoMoreInteractions(paymentInstrumentRestConnector);
    }

    @Test
    void suspend_ok() {
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        Mockito.doAnswer(
                        invocationOnMock -> {
                            testWallet.setStatus(WalletStatus.SUSPENDED);
                            return null;
                        })
                .when(walletUpdatesRepositoryMock)
                .suspendWallet(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString(), any());

        Mockito.doNothing()
                .when(notificationProducer)
                .sendNotification(any(NotificationQueueDTO.class));

        walletService.suspendWallet(INITIATIVE_ID, USER_ID);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(1))
                .suspendWallet(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString(), any());
        Mockito.verify(onboardingRestConnector, Mockito.times(1))
                .suspendOnboarding(INITIATIVE_ID, USER_ID);
        Mockito.verify(notificationProducer, Mockito.times(1))
                .sendNotification(any());
        assertEquals(WalletStatus.SUSPENDED, testWallet.getStatus());
    }

    @Test
    void suspend_walletUnsubscribed() {
        // Given
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(TEST_WALLET_UNSUBSCRIBED));

        // When
        UserUnsubscribedException exception = assertThrows(UserUnsubscribedException.class,
                () -> walletService.suspendWallet(INITIATIVE_ID, USER_ID));

        // Then
        assertEquals(USER_UNSUBSCRIBED, exception.getCode());
        assertEquals(String.format(ERROR_UNSUBSCRIBED_INITIATIVE_MSG, INITIATIVE_ID), exception.getMessage());

        verify(walletRepositoryMock, times(1)).findById(any());
        verifyNoMoreInteractions(walletRepositoryMock);
        verifyNoInteractions(walletUpdatesRepositoryMock);
        verifyNoInteractions(onboardingRestConnector);
        verifyNoInteractions(timelineMapper);
        verifyNoInteractions(timelineProducer);
        verifyNoInteractions(notificationProducer);
    }

    @Test
    void suspend_ko() {
        // Given
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        Mockito.doAnswer(
                        invocationOnMock -> {
                            testWallet.setStatus(WalletStatus.SUSPENDED);
                            return null;
                        })
                .when(walletUpdatesRepositoryMock)
                .suspendWallet(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString(), any());

        doThrow(new OnboardingInvocationException(ERROR_ONBOARDING_INVOCATION_MSG))
                .when(onboardingRestConnector)
                .suspendOnboarding(anyString(), anyString());

        // When
        OnboardingInvocationException exception = assertThrows(OnboardingInvocationException.class,
                () -> walletService.suspendWallet(INITIATIVE_ID, USER_ID));

        // Then
        assertEquals(GENERIC_ERROR, exception.getCode());
        assertEquals(ERROR_ONBOARDING_INVOCATION_MSG, exception.getMessage());

        verify(walletRepositoryMock, times(1)).findById(any());
        verify(walletUpdatesRepositoryMock, times(1)).suspendWallet(any(), any(), any(), any());
        verify(onboardingRestConnector, times(1)).suspendOnboarding(any(), any());
        verify(walletRepositoryMock, times(1)).save(any());
        verifyNoMoreInteractions(walletRepositoryMock);
        verifyNoInteractions(timelineMapper);
        verifyNoInteractions(timelineProducer);
        verifyNoInteractions(notificationProducer);

    }

    @Test
    void suspend_idemp() {
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(TEST_WALLET_SUSPENDED));

        walletService.suspendWallet(INITIATIVE_ID, USER_ID);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(0))
                .suspendWallet(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString(), any());
        Mockito.verify(onboardingRestConnector, Mockito.times(0))
                .suspendOnboarding(INITIATIVE_ID, USER_ID);
    }

    @ParameterizedTest
    @ValueSource(strings = {WALLET_REFUNDABLE, WALLET_NOT_REFUNDABLE, WALLET_NOT_REFUNDABLE_ONLY_IBAN, WALLET_NOT_REFUNDABLE_ONLY_INSTRUMENT})
    void readmit_ok(String status) {
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(TEST_WALLET_SUSPENDED));

        Mockito.doAnswer(
                        invocationOnMock -> {
                            TEST_WALLET_SUSPENDED.setStatus(status);
                            return null;
                        })
                .when(walletUpdatesRepositoryMock)
                .readmitWallet(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString(), any());

        Mockito.doNothing()
                .when(notificationProducer)
                .sendNotification(any(NotificationQueueDTO.class));

        walletService.readmitWallet(INITIATIVE_ID, USER_ID);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(1))
                .readmitWallet(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString(), any());
        Mockito.verify(onboardingRestConnector, Mockito.times(1))
                .readmitOnboarding(INITIATIVE_ID, USER_ID);
        Mockito.verify(notificationProducer, Mockito.times(1))
                .sendNotification(any());
        assertEquals(status, TEST_WALLET_SUSPENDED.getStatus());
    }

    @Test
    void readmit_walletUnsubscribed() {
        // Given
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(TEST_WALLET_UNSUBSCRIBED));

        // When
        UserUnsubscribedException exception = assertThrows(UserUnsubscribedException.class,
                () -> walletService.readmitWallet(INITIATIVE_ID, USER_ID));

        // Then
        assertEquals(USER_UNSUBSCRIBED, exception.getCode());
        assertEquals(String.format(ERROR_UNSUBSCRIBED_INITIATIVE_MSG, INITIATIVE_ID), exception.getMessage());

        verify(walletRepositoryMock, times(1)).findById(any());
        verifyNoMoreInteractions(walletRepositoryMock);
        verifyNoInteractions(walletUpdatesRepositoryMock);
        verifyNoInteractions(onboardingRestConnector);
    }

    @Test
    void readmit_ko() {
        // Given
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        Mockito.doAnswer(
                        invocationOnMock -> {
                            testWallet.setStatus(WalletStatus.NOT_REFUNDABLE.name());
                            return null;
                        })
                .when(walletUpdatesRepositoryMock)
                .readmitWallet(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString(), any());

        doThrow(new OnboardingInvocationException(ERROR_ONBOARDING_INVOCATION_MSG))
                .when(onboardingRestConnector)
                .readmitOnboarding(anyString(), anyString());

        // When
        OnboardingInvocationException exception = assertThrows(OnboardingInvocationException.class,
                () -> walletService.readmitWallet(INITIATIVE_ID, USER_ID));

        // Then
        assertEquals(GENERIC_ERROR, exception.getCode());
        assertEquals(ERROR_ONBOARDING_INVOCATION_MSG, exception.getMessage());

        verify(walletRepositoryMock, times(1)).findById(any());
        verify(walletUpdatesRepositoryMock, times(1)).readmitWallet(any(), any(), any(), any());
        verifyNoMoreInteractions(walletUpdatesRepositoryMock);
        verify(walletRepositoryMock, times(1)).save(any());
        verifyNoMoreInteractions(walletRepositoryMock);
    }

    @Test
    void readmit_idemp() {
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(TEST_WALLET_REFUNDABLE));

        Mockito.doAnswer(
                        invocationOnMock -> {
                            testWallet.setStatus(WalletStatus.REFUNDABLE.name());
                            return null;
                        })
                .when(walletUpdatesRepositoryMock)
                .readmitWallet(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString(), any());

        walletService.readmitWallet(INITIATIVE_ID, USER_ID);

        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(1))
                .readmitWallet(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString(), any());
        Mockito.verify(onboardingRestConnector, Mockito.times(1))
                .readmitOnboarding(INITIATIVE_ID, USER_ID);
        assertEquals(WalletStatus.REFUNDABLE.name(), TEST_WALLET_REFUNDABLE.getStatus());
    }

    @Test
    void readmit_initiativeTypeDiscount() {
        // Given
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(TEST_WALLET_SUSPENDED));

        Mockito.doAnswer(
                        invocationOnMock -> {
                            TEST_WALLET_SUSPENDED.setStatus("SUSPENDED");
                            TEST_WALLET_SUSPENDED.setInitiativeRewardType("DISCOUNT");
                            return null;
                        })
                .when(walletUpdatesRepositoryMock)
                .readmitWallet(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString(), any());

        Mockito.doNothing()
                .when(notificationProducer)
                .sendNotification(any(NotificationQueueDTO.class));

        // When
        walletService.readmitWallet(INITIATIVE_ID, USER_ID);

        // Then
        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(1))
                .readmitWallet(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.anyString(), any());
        Mockito.verify(onboardingRestConnector, Mockito.times(1))
                .readmitOnboarding(INITIATIVE_ID, USER_ID);
        Mockito.verify(notificationProducer, Mockito.times(1))
                .sendNotification(any());
    }

    @ParameterizedTest
    @MethodSource("operationTypeAndInvocationTimes")
    void processCommand(String operationType, int times) {
        // Given
        final QueueCommandOperationDTO queueCommandOperationDTO = QueueCommandOperationDTO.builder()
                .entityId(INITIATIVE_ID)
                .operationType(operationType)
                .operationTime(LocalDateTime.now().minusMinutes(5))
                .build();
        final List<Wallet> deletedPage = createWalletPage(20);

        if (times == 2) {
            final List<Wallet> walletPage = createWalletPage(PAGINATION_VALUE);
            when(walletUpdatesRepositoryMock.deletePaged(queueCommandOperationDTO.getEntityId(), Integer.parseInt(pagination)))
                    .thenReturn(walletPage)
                    .thenReturn(deletedPage);
        } else {
            when(walletUpdatesRepositoryMock.deletePaged(queueCommandOperationDTO.getEntityId(), Integer.parseInt(pagination)))
                    .thenReturn(deletedPage);
        }

        // When
        if (times == 2) {
            Thread.currentThread().interrupt();
        }
        walletService.processCommand(queueCommandOperationDTO);

        // Then
        Mockito.verify(walletUpdatesRepositoryMock, Mockito.times(times)).deletePaged(queueCommandOperationDTO.getEntityId(), Integer.parseInt(pagination));
    }

    private static Stream<Arguments> operationTypeAndInvocationTimes() {
        return Stream.of(
                Arguments.of(OPERATION_TYPE_DELETE_INITIATIVE, 1),
                Arguments.of(OPERATION_TYPE_DELETE_INITIATIVE, 2),
                Arguments.of("OPERATION_TYPE_TEST", 0)
        );
    }

    private List<Wallet> createWalletPage(int pageSize) {
        List<Wallet> walletPage = new ArrayList<>();

        for (int i = 0; i < pageSize; i++) {
            walletPage.add(Wallet.builder()
                    .id(ID_WALLET + i)
                    .initiativeId(INITIATIVE_ID)
                    .build());
        }

        return walletPage;
    }

    @Test
    void enrollInstrumentCode_ok() {
        testWallet.setInitiativeRewardType(WalletConstants.INITIATIVE_REWARD_TYPE_DISCOUNT);
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        testWallet.setIban(null);
        testWallet.setNInstr(0);
        testWallet.setInitiativeEndDate(LocalDate.MAX);

        Mockito.doNothing()
                .when(paymentInstrumentRestConnector)
                .enrollInstrumentCode(Mockito.any(InstrumentCallBodyDTO.class));


        walletService.enrollInstrumentCode(INITIATIVE_ID, USER_ID, CHANNEL_APP_IO);

        assertEquals(WalletStatus.NOT_REFUNDABLE.name(), testWallet.getStatus());
        assertEquals(0, testWallet.getNInstr());
    }

    @Test
    void enrollInstrumentCode_ko_exception() {
        // Given
        testWallet.setInitiativeEndDate(LocalDate.MAX);
        testWallet.setInitiativeRewardType(WalletConstants.INITIATIVE_REWARD_TYPE_DISCOUNT);
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        Mockito.when(timelineMapper.ackToTimeline(INSTRUMENT_ACK_DTO_ADD_INSTRUMENT)).thenReturn(TEST_OPERATION_DTO);

        Mockito.doThrow(new IDPayCodeNotFoundException(IDPAYCODE_NOT_FOUND_MSG))
                .when(paymentInstrumentRestConnector)
                .enrollInstrumentCode(Mockito.any(InstrumentCallBodyDTO.class));

        // When
        IDPayCodeNotFoundException exception = assertThrows(IDPayCodeNotFoundException.class,
                () -> walletService.enrollInstrumentCode(INITIATIVE_ID, USER_ID, CHANNEL_APP_IO));

        // Then
        assertEquals(IDPAYCODE_NOT_FOUND, exception.getCode());
        assertEquals(IDPAYCODE_NOT_FOUND_MSG, exception.getMessage());

        verify(walletRepositoryMock, times(1)).findById(any());
        verifyNoMoreInteractions(walletRepositoryMock);
        verify(paymentInstrumentRestConnector, times(1)).enrollInstrumentCode(any());
        verifyNoMoreInteractions(walletRepositoryMock);
        verify(timelineMapper, times(1)).ackToTimeline(any());
        verify(timelineProducer, times(1)).sendEvent(any());
    }

    @Test
    void enrollInstrumentCode_ko_initiative_refund() {
        // Given
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        // When
        EnrollmentNotAllowedException exception = assertThrows(EnrollmentNotAllowedException.class,
                () -> walletService.enrollInstrumentCode(INITIATIVE_ID, USER_ID, CHANNEL_APP_IO));

        // Then
        assertEquals(ENROLL_INSTRUMENT_REFUND_INITIATIVE, exception.getCode());
        assertEquals(String.format(PAYMENT_INSTRUMENT_ENROLL_NOT_ALLOWED_REFUND_MSG, INITIATIVE_ID), exception.getMessage());

        verify(walletRepositoryMock, times(1)).findById(any());
        verifyNoMoreInteractions(walletRepositoryMock);
        verifyNoInteractions(paymentInstrumentRestConnector);
        verifyNoInteractions(timelineMapper);
        verifyNoInteractions(timelineProducer);
    }

    @Test
    void enrollInstrumentCode_ko_unsubscribed() {
        // Given
        testWallet.setInitiativeEndDate(LocalDate.MAX);
        testWallet.setStatus(WalletStatus.UNSUBSCRIBED);
        testWallet.setInitiativeRewardType(WalletConstants.INITIATIVE_REWARD_TYPE_DISCOUNT);
        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        // When
        UserUnsubscribedException exception = assertThrows(UserUnsubscribedException.class,
                () -> walletService.enrollInstrumentCode(INITIATIVE_ID, USER_ID, CHANNEL_APP_IO));

        // Then
        assertEquals(USER_UNSUBSCRIBED, exception.getCode());
        assertEquals(String.format(ERROR_UNSUBSCRIBED_INITIATIVE_MSG, INITIATIVE_ID), exception.getMessage());

        verify(walletRepositoryMock, times(1)).findById(any());
        verifyNoMoreInteractions(walletRepositoryMock);
        verifyNoInteractions(paymentInstrumentRestConnector);
        verifyNoInteractions(timelineMapper);
        verifyNoInteractions(timelineProducer);
    }

    @Test
    void processTransaction_ok_expired() {

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        Mockito.doAnswer(
                        invocationOnMock -> {
                            testWallet.setRequestUnsubscribeDate(LocalDateTime.now());
                            testWallet.setStatus(WalletStatus.UNSUBSCRIBED);
                            return null;
                        })
                .when(walletRepositoryMock)
                .save(any(Wallet.class));

        Assertions.assertDoesNotThrow(() ->
                walletService.processTransaction(
                        MessageBuilder.withPayload(objectMapper.writeValueAsString(REWARD_TRX_DTO_EXPIRED)).build()));

        verify(paymentInstrumentRestConnector).disableAllInstrument(any());
        verify(onboardingRestConnector).disableOnboarding(any());

    }

    @Test
    void processTransaction_ok_refunded() {

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        Mockito.doAnswer(
                        invocationOnMock -> {
                            testWallet.setRequestUnsubscribeDate(LocalDateTime.now());
                            testWallet.setStatus(WalletStatus.UNSUBSCRIBED);
                            return null;
                        })
                .when(walletRepositoryMock)
                .save(any(Wallet.class));

        Assertions.assertDoesNotThrow(() ->
                walletService.processTransaction(
                        MessageBuilder.withPayload(objectMapper.writeValueAsString(REWARD_TRX_DTO_REFUNDED)).build()));

        verify(paymentInstrumentRestConnector).disableAllInstrument(any());
        verify(onboardingRestConnector).disableOnboarding(any());

    }

    @Test
    void processTransaction_ko_expired() {

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        Mockito.doAnswer(
                        invocationOnMock -> {
                            testWallet.setRequestUnsubscribeDate(LocalDateTime.now());
                            testWallet.setStatus(WalletStatus.UNSUBSCRIBED);
                            return null;
                        })
                .when(walletRepositoryMock)
                .save(any(Wallet.class));

        doThrow(new RuntimeException("test")).doNothing().when(paymentInstrumentRestConnector)
                .disableAllInstrument(any());

        Assertions.assertDoesNotThrow(() ->
                walletService.processTransaction(
                        MessageBuilder.withPayload(objectMapper.writeValueAsString(REWARD_TRX_DTO_EXPIRED)).build()));

        verify(paymentInstrumentRestConnector).disableAllInstrument(any());
        verify(errorProducer).sendEvent(any());

    }

    @Test
    void processTransaction_ko_unparsable() {

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(testWallet));

        Mockito.doAnswer(
                        invocationOnMock -> {
                            testWallet.setRequestUnsubscribeDate(LocalDateTime.now());
                            testWallet.setStatus(WalletStatus.UNSUBSCRIBED);
                            return null;
                        })
                .when(walletRepositoryMock)
                .save(any(Wallet.class));

        doThrow(new RuntimeException("test")).doNothing().when(paymentInstrumentRestConnector)
                .disableAllInstrument(any());

        Assertions.assertDoesNotThrow(() ->
                walletService.processTransaction(
                        MessageBuilder.withPayload("AAAAA").build()));

        verifyNoInteractions(paymentInstrumentRestConnector);
        verify(errorProducer).sendEvent(any());

    }

}
