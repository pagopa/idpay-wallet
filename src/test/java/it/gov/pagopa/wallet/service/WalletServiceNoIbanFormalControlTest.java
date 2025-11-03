package it.gov.pagopa.wallet.service;

import it.gov.pagopa.common.config.ObjectMapperConfig;
import it.gov.pagopa.wallet.connector.InitiativeRestConnector;
import it.gov.pagopa.wallet.connector.OnboardingRestConnector;
import it.gov.pagopa.wallet.connector.PaymentInstrumentRestConnector;
import it.gov.pagopa.wallet.connector.PaymentRestConnector;
import it.gov.pagopa.wallet.dto.mapper.TimelineMapper;
import it.gov.pagopa.wallet.dto.mapper.WalletMapper;
import it.gov.pagopa.wallet.dto.payment.TransactionBarCodeCreationRequest;
import it.gov.pagopa.wallet.dto.payment.TransactionBarCodeEnrichedResponse;
import it.gov.pagopa.wallet.enums.SyncTrxStatus;
import it.gov.pagopa.wallet.enums.WalletStatus;
import it.gov.pagopa.wallet.event.producer.ErrorProducer;
import it.gov.pagopa.wallet.event.producer.IbanProducer;
import it.gov.pagopa.wallet.event.producer.NotificationProducer;
import it.gov.pagopa.wallet.event.producer.TimelineProducer;
import it.gov.pagopa.wallet.exception.custom.InvalidIbanException;
import it.gov.pagopa.wallet.model.Wallet;
import it.gov.pagopa.wallet.repository.WalletRepository;
import it.gov.pagopa.wallet.repository.WalletUpdatesRepository;
import it.gov.pagopa.wallet.utils.AuditUtilities;
import it.gov.pagopa.wallet.utils.Utilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.IBAN_NOT_ITALIAN;
import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionMessage.ERROR_IBAN_NOT_ITALIAN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = {WalletServiceImpl.class, ObjectMapperConfig.class})
@TestPropertySource(
        locations = "classpath:application.yml",
        properties = {
                "app.iban.formalControl=false",
                "app.delete.paginationSize=100",
                "app.delete.delayTime=1000"
        })
class WalletServiceNoIbanFormalControlTest {

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
    Utilities utilities;
    @MockitoBean
    PaymentRestConnector paymentRestConnector;

    private static final String USER_ID = "TEST_USER_ID";
    private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";
    private static final String INITIATIVE_NAME = "TEST_INITIATIVE_NAME";
    private static final String CHANNEL = "CHANNEL";
    private static final String IBAN_OK = "IT09P0000005138205493205499";
    private static final String IBAN_KO_NOT_IT = "GB29NWBK60161331926819";
    private static final String DESCRIPTION_OK = "conto cointestato";
    private static final String ID_WALLET = "TEST_USER_ID_TEST_INITIATIVE_ID";
    private static final LocalDateTime TEST_DATE = LocalDateTime.now();
    private static final LocalDate TEST_DATE_ONLY_DATE = LocalDate.now();
    private static final Long TEST_AMOUNT = 200L;
    private static final Long TEST_ACCRUED = 4000L;
    private static final Long TEST_REFUNDED = 0L;

    private static final String INITIATIE_REWARD_TYPE_REFUND = "REFUND";

    private static final Wallet TEST_WALLET =
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
                    .initiativeRewardType(INITIATIE_REWARD_TYPE_REFUND)
                    .build();
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
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

    @Test
    void enrollIban_ok_only_iban() {
        TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE.name());
        TEST_WALLET.setNInstr(0);
        TEST_WALLET.setInitiativeEndDate(LocalDate.MAX);

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(TEST_WALLET));

        Mockito.doAnswer(
                        invocationOnMock -> {
                            TEST_WALLET.setIban(IBAN_OK);
                            TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_IBAN.name());
                            return null;
                        })
                .when(walletUpdatesRepositoryMock)
                .enrollIban(Mockito.eq(INITIATIVE_ID), Mockito.eq(USER_ID), Mockito.eq(IBAN_OK), Mockito.anyString());

        walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, CHANNEL, DESCRIPTION_OK);

        assertEquals(WalletStatus.NOT_REFUNDABLE_ONLY_IBAN.name(), TEST_WALLET.getStatus());
    }

    @Test
    void enrollIban_ko_iban_not_italian() {
        // Given
        TEST_WALLET.setIban(null);
        TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name());
        TEST_WALLET.setInitiativeEndDate(LocalDate.MAX);

        Mockito.when(walletRepositoryMock.findById(ID_WALLET))
                .thenReturn(Optional.of(TEST_WALLET));

        // When
        InvalidIbanException exception = assertThrows(InvalidIbanException.class,
                () -> walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_KO_NOT_IT, CHANNEL, DESCRIPTION_OK));

        // Then
        assertEquals(IBAN_NOT_ITALIAN, exception.getCode());
        assertEquals(String.format(ERROR_IBAN_NOT_ITALIAN, IBAN_KO_NOT_IT), exception.getMessage());
    }
}
