package it.gov.pagopa.wallet.service;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import it.gov.pagopa.wallet.connector.InitiativeRestConnector;
import it.gov.pagopa.wallet.connector.OnboardingRestConnector;
import it.gov.pagopa.wallet.connector.PaymentInstrumentRestConnector;
import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.Counters;
import it.gov.pagopa.wallet.dto.DeactivationBodyDTO;
import it.gov.pagopa.wallet.dto.EnrollmentStatusDTO;
import it.gov.pagopa.wallet.dto.EvaluationDTO;
import it.gov.pagopa.wallet.dto.IbanQueueDTO;
import it.gov.pagopa.wallet.dto.IbanQueueWalletDTO;
import it.gov.pagopa.wallet.dto.InitiativeListDTO;
import it.gov.pagopa.wallet.dto.InstrumentAckDTO;
import it.gov.pagopa.wallet.dto.InstrumentCallBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentIssuerCallDTO;
import it.gov.pagopa.wallet.dto.InstrumentIssuerDTO;
import it.gov.pagopa.wallet.dto.NotificationQueueDTO;
import it.gov.pagopa.wallet.dto.QueueOperationDTO;
import it.gov.pagopa.wallet.dto.RefundDTO;
import it.gov.pagopa.wallet.dto.RewardDTO;
import it.gov.pagopa.wallet.dto.RewardTransactionDTO;
import it.gov.pagopa.wallet.dto.UnsubscribeCallDTO;
import it.gov.pagopa.wallet.dto.WalletDTO;
import it.gov.pagopa.wallet.dto.WalletPIBodyDTO;
import it.gov.pagopa.wallet.dto.WalletPIDTO;
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
import org.iban4j.IbanFormatException;
import org.iban4j.InvalidCheckDigitException;
import org.iban4j.UnsupportedCountryException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private static final String USER_ID = "TEST_USER_ID";
    private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";
    private static final String INITIATIVE_NAME = "TEST_INITIATIVE_NAME";
    private static final String MASKED_PAN = "masked_pan";
    private static final String BRAND_LOGO = "brand_logo";
    private static final String CIRCUIT_TYPE = "circuit_type";

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
    private static final LocalDate TEST_DATE_ONLY_DATE = LocalDate.now();
    private static final BigDecimal TEST_AMOUNT = BigDecimal.valueOf(2.00);
    private static final BigDecimal TEST_ACCRUED = BigDecimal.valueOf(40.00);
    private static final BigDecimal TEST_REFUNDED = BigDecimal.valueOf(0.00);

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

    private static final Wallet TEST_WALLET_UNSUBSCRIBED =
            Wallet.builder()
                    .userId(USER_ID)
                    .initiativeId(INITIATIVE_ID)
                    .initiativeName(INITIATIVE_NAME)
                    .acceptanceDate(TEST_DATE)
                    .status(WalletStatus.UNSUBSCRIBED)
                    .endDate(TEST_DATE_ONLY_DATE)
                    .amount(TEST_AMOUNT)
                    .accrued(TEST_ACCRUED)
                    .refunded(TEST_REFUNDED)
                    .build();

  private static final WalletDTO WALLET_DTO =
      new WalletDTO(
          INITIATIVE_ID,
          INITIATIVE_NAME,
          WalletStatus.NOT_REFUNDABLE.name(),
          IBAN_OK,
          TEST_DATE_ONLY_DATE,
          0,
          TEST_AMOUNT,
          TEST_ACCRUED,
          TEST_REFUNDED,
          TEST_DATE);

  private static final WalletDTO WALLET_ISSUER_DTO =
      new WalletDTO(null, null, null, null, null, 0, TEST_AMOUNT, TEST_ACCRUED, TEST_REFUNDED, TEST_DATE);

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
                    INITIATIVE_ID,
                    INITIATIVE_ID,
                    TEST_DATE_ONLY_DATE,
                    INITIATIVE_ID,
                    WalletConstants.STATUS_ONBOARDING_KO,
                    TEST_DATE,
                    TEST_DATE,
                    List.of(),
                    new BigDecimal(500));

    private static final EvaluationDTO OUTCOME_OK =
            new EvaluationDTO(
                    USER_ID,
                    INITIATIVE_ID,
                    INITIATIVE_ID,
                    TEST_DATE_ONLY_DATE,
                    INITIATIVE_ID,
                    WalletConstants.STATUS_ONBOARDING_OK,
                    TEST_DATE,
                    TEST_DATE,
                    List.of(),
                    new BigDecimal(500));


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
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(Mockito.anyString(),Mockito.anyString())).thenReturn(Optional.of(TEST_WALLET));
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
  void createWallet() {
    Mockito.when(walletMapper.map(Mockito.any())).thenReturn(TEST_WALLET);
    walletService.createWallet(OUTCOME_OK);
    Mockito.verify(walletRepositoryMock, Mockito.times(1)).save(Mockito.any());
    Mockito.verify(timelineProducer, Mockito.times(1)).sendEvent(Mockito.any());
  }

    @Test
    void createWallet_doNoting() {
        walletService.createWallet(OUTCOME_KO);
        Mockito.verify(walletRepositoryMock, Mockito.times(0)).save(Mockito.any());
        Mockito.verify(timelineProducer, Mockito.times(0)).sendEvent(Mockito.any());
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
                .sendCheckIban(Mockito.any(NotificationQueueDTO.class));

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
                .sendCheckIban(Mockito.any(NotificationQueueDTO.class));
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
                        "ACCEPTED",
                        4000L,
                        4000L,
                        LocalDateTime.now(),
                        null,
                        null,
                        1L,
                        LocalDate.now(),
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
                        "ACCEPTED",
                        4000L,
                        4000L,
                        LocalDateTime.now(),
                        null,
                        null,
                        1L,
                        LocalDate.now(),
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
                        "REJECTED",
                        -4000L,
                        4000L,
                        LocalDateTime.now(),
                        null,
                        null,
                        2L,
                        LocalDate.now(),
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
                        "REJECTED",
                        0L,
                        4000L,
                        LocalDateTime.now(),
                        null,
                        null,
                        1L,
                        LocalDate.now(),
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
                        "REJECTED",
                        0L,
                        4000L,
                        LocalDateTime.now(),
                        null,
                        null,
                        2L,
                        LocalDate.now(),
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
                        "REJECTED",
                        0L,
                        4000L,
                        LocalDateTime.now(),
                        null,
                        null,
                        2L,
                        LocalDate.now(),
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
    void enrollInstrumentIssuer_ko_feignexception() {

        TEST_WALLET.setEndDate(LocalDate.MAX);

        final InstrumentIssuerDTO instrument =
                new InstrumentIssuerDTO("hpan", CHANNEL, "VISA", "VISA", "***");

        Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
                .thenReturn(Optional.of(TEST_WALLET));


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
}
