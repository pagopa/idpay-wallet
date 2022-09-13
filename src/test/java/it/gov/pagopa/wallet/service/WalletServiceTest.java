package it.gov.pagopa.wallet.service;

import static it.gov.pagopa.wallet.constants.WalletConstants.STATUS_KO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import it.gov.pagopa.wallet.connector.OnboardingRestConnector;
import it.gov.pagopa.wallet.connector.PaymentInstrumentRestConnector;
import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.Counters;
import it.gov.pagopa.wallet.dto.DeactivationBodyDTO;
import it.gov.pagopa.wallet.dto.EnrollmentStatusDTO;
import it.gov.pagopa.wallet.dto.EvaluationDTO;
import it.gov.pagopa.wallet.dto.IbanQueueDTO;
import it.gov.pagopa.wallet.dto.IbanQueueWalletDTO;
import it.gov.pagopa.wallet.dto.InitiativeDTO;
import it.gov.pagopa.wallet.dto.InitiativeListDTO;
import it.gov.pagopa.wallet.dto.InstrumentCallBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentResponseDTO;
import it.gov.pagopa.wallet.dto.NotificationQueueDTO;
import it.gov.pagopa.wallet.dto.QueueOperationDTO;
import it.gov.pagopa.wallet.dto.RewardDTO;
import it.gov.pagopa.wallet.dto.RewardTransactionDTO;
import it.gov.pagopa.wallet.dto.UnsubscribeCallDTO;
import it.gov.pagopa.wallet.dto.mapper.TimelineMapper;
import it.gov.pagopa.wallet.dto.mapper.WalletMapper;
import it.gov.pagopa.wallet.enums.WalletStatus;
import it.gov.pagopa.wallet.event.producer.IbanProducer;
import it.gov.pagopa.wallet.event.producer.NotificationProducer;
import it.gov.pagopa.wallet.event.producer.TimelineProducer;
import it.gov.pagopa.wallet.exception.WalletException;
import it.gov.pagopa.wallet.model.Wallet;
import it.gov.pagopa.wallet.repository.WalletRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = WalletServiceImpl.class)
class WalletServiceTest {

  @MockBean IbanProducer ibanProducer;
  @MockBean TimelineProducer timelineProducer;
  @MockBean NotificationProducer notificationProducer;
  @MockBean WalletRepository walletRepositoryMock;
  @MockBean PaymentInstrumentRestConnector paymentInstrumentRestConnector;
  @MockBean OnboardingRestConnector onboardingRestConnector;
  @MockBean WalletMapper walletMapper;
  @MockBean TimelineMapper timelineMapper;
  @Autowired WalletService walletService;

  private static final String USER_ID = "TEST_USER_ID";
  private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";
  private static final String INITIATIVE_NAME = "TEST_INITIATIVE_NAME";

  private static final String INITIATIVE_ID_FAIL = "FAIL";
  private static final String HPAN = "TEST_HPAN";
  private static final String IBAN_OK = "IT09P3608105138205493205495";
  private static final String IBAN_OK_OTHER = "IT09P3608105138205493205494";
  private static final String IBAN_KO_NOT_IT = "GB29NWBK60161331926819";
  private static final String IBAN_WRONG = "it99C1234567890123456789012222";
  private static final String IBAN_WRONG_DIGIT = "IT09P3608105138205493205496";
  private static final String DESCRIPTION_OK = "conto cointestato";
  private static final LocalDateTime TEST_DATE = LocalDateTime.now();
  private static final BigDecimal TEST_AMOUNT = BigDecimal.valueOf(2.00);
  private static final BigDecimal TEST_ACCRUED = BigDecimal.valueOf(0.00);
  private static final BigDecimal TEST_REFUNDED = BigDecimal.valueOf(0.00);
  private static final int TEST_COUNT = 2;
  private static final int TEST_COUNT_IDEMP = 1;

  private static final Wallet TEST_WALLET =
      Wallet.builder()
          .userId(USER_ID)
          .initiativeId(INITIATIVE_ID)
          .initiativeName(INITIATIVE_NAME)
          .acceptanceDate(TEST_DATE)
          .status(WalletStatus.NOT_REFUNDABLE.name())
          .endDate(TEST_DATE)
          .amount(TEST_AMOUNT)
          .accrued(TEST_ACCRUED)
          .refunded(TEST_REFUNDED)
          .build();

  private static final Wallet TEST_WALLET_UNSUBSCRIBED =
      Wallet.builder()
          .userId(USER_ID)
          .initiativeId(INITIATIVE_ID)
          .initiativeName(INITIATIVE_NAME)
          .acceptanceDate(TEST_DATE)
          .status(WalletStatus.UNSUBSCRIBED)
          .endDate(TEST_DATE)
          .amount(TEST_AMOUNT)
          .accrued(TEST_ACCRUED)
          .refunded(TEST_REFUNDED)
          .build();

  private static final InstrumentResponseDTO INSTRUMENT_RESPONSE_DTO =
      new InstrumentResponseDTO(TEST_COUNT);

  private static final InstrumentResponseDTO INSTRUMENT_RESPONSE_DTO_IDEMP =
      new InstrumentResponseDTO(TEST_COUNT_IDEMP);

  private static final InitiativeDTO INITIATIVE_DTO =
      new InitiativeDTO(
          INITIATIVE_ID,
          INITIATIVE_NAME,
          WalletStatus.NOT_REFUNDABLE.name(),
          IBAN_OK,
          TEST_DATE.toString(),
          "0",
          String.valueOf(TEST_AMOUNT),
          String.valueOf(TEST_ACCRUED),
          String.valueOf(TEST_REFUNDED));

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
          TEST_DATE,
          INITIATIVE_ID,
          WalletConstants.STATUS_ONBOARDING_KO,
          TEST_DATE,
          List.of(),
          new BigDecimal(500),
          INITIATIVE_ID);

  private static final EvaluationDTO OUTCOME_OK =
      new EvaluationDTO(
          USER_ID,
          INITIATIVE_ID,
          INITIATIVE_ID,
          TEST_DATE,
          INITIATIVE_ID,
          WalletConstants.STATUS_ONBOARDING_OK,
          TEST_DATE,
          List.of(),
          new BigDecimal(500),
          INITIATIVE_ID);

  @Test
  void enrollInstrument_ok() {
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    TEST_WALLET.setIban(null);
    TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE.name());
    TEST_WALLET.setNInstr(0);

    Mockito.when(
            paymentInstrumentRestConnector.enrollInstrument(
                Mockito.any(InstrumentCallBodyDTO.class)))
        .thenReturn(INSTRUMENT_RESPONSE_DTO);

    try {
      walletService.enrollInstrument(INITIATIVE_ID, USER_ID, HPAN);
    } catch (WalletException e) {
      Assertions.fail();
    }
    assertEquals(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name(), TEST_WALLET.getStatus());
    assertEquals(TEST_COUNT, TEST_WALLET.getNInstr());
  }

  @Test
  void enrollInstrument_idemp() {
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    TEST_WALLET.setIban(null);
    TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name());
    TEST_WALLET.setNInstr(1);

    Mockito.when(
            paymentInstrumentRestConnector.enrollInstrument(
                Mockito.any(InstrumentCallBodyDTO.class)))
        .thenReturn(INSTRUMENT_RESPONSE_DTO_IDEMP);

    try {
      walletService.enrollInstrument(INITIATIVE_ID, USER_ID, HPAN);
    } catch (WalletException e) {
      Assertions.fail();
    }
    assertEquals(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name(), TEST_WALLET.getStatus());
    assertEquals(TEST_COUNT_IDEMP, TEST_WALLET.getNInstr());
  }

  @Test
  void enrollInstrument_ok_with_iban() {
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    TEST_WALLET.setIban(IBAN_OK);
    TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_IBAN.name());
    TEST_WALLET.setNInstr(0);

    Mockito.when(
            paymentInstrumentRestConnector.enrollInstrument(
                Mockito.any(InstrumentCallBodyDTO.class)))
        .thenReturn(INSTRUMENT_RESPONSE_DTO);

    try {
      walletService.enrollInstrument(INITIATIVE_ID, USER_ID, HPAN);
    } catch (WalletException e) {
      Assertions.fail();
    }

    Mockito.doNothing().when(timelineProducer).sendEvent(Mockito.any(QueueOperationDTO.class));

    assertEquals(WalletStatus.REFUNDABLE.name(), TEST_WALLET.getStatus());
    assertEquals(TEST_COUNT, TEST_WALLET.getNInstr());
  }

  @Test
  void enrollInstrument_ok_with_instrument() {
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name());
    TEST_WALLET.setIban(null);

    Mockito.when(
            paymentInstrumentRestConnector.enrollInstrument(
                Mockito.any(InstrumentCallBodyDTO.class)))
        .thenReturn(INSTRUMENT_RESPONSE_DTO);

    Mockito.doNothing().when(timelineProducer).sendEvent(Mockito.any(QueueOperationDTO.class));

    try {
      walletService.enrollInstrument(INITIATIVE_ID, USER_ID, HPAN);
    } catch (WalletException e) {
      Assertions.fail();
    }
    assertEquals(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name(), TEST_WALLET.getStatus());
    assertEquals(TEST_COUNT, TEST_WALLET.getNInstr());
  }

  @Test
  void enrollInstrument_ko_feignexception() {
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    Request request =
        Request.create(Request.HttpMethod.PUT, "url", new HashMap<>(), null, new RequestTemplate());

    Mockito.doThrow(new FeignException.BadRequest("", request, new byte[0], null))
        .when(paymentInstrumentRestConnector)
        .enrollInstrument(Mockito.any(InstrumentCallBodyDTO.class));

    try {
      walletService.enrollInstrument(INITIATIVE_ID, USER_ID, HPAN);
      Assertions.fail();
    } catch (WalletException e) {
      assertEquals(HttpStatus.BAD_REQUEST.value(), e.getCode());
    }
  }

  @Test
  void enrollInstrument_not_found() {
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.empty());
    try {
      walletService.enrollInstrument(INITIATIVE_ID, USER_ID, HPAN);
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
      walletService.enrollInstrument(INITIATIVE_ID, USER_ID, HPAN);
      Assertions.fail();
    } catch (WalletException e) {
      assertEquals(HttpStatus.BAD_REQUEST.value(), e.getCode());
      assertEquals(WalletConstants.ERROR_INITIATIVE_UNSUBSCRIBED, e.getMessage());
    }
  }

  @Test
  void deleteInstrument_ok_with_iban() {
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    TEST_WALLET.setIban(IBAN_OK);
    TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_IBAN.name());
    TEST_WALLET.setNInstr(1);

    Mockito.when(
            paymentInstrumentRestConnector.deleteInstrument(Mockito.any(DeactivationBodyDTO.class)))
        .thenReturn(INSTRUMENT_RESPONSE_DTO);

    try {
      walletService.deleteInstrument(INITIATIVE_ID, USER_ID, HPAN);
    } catch (WalletException e) {
      Assertions.fail();
    }

    Mockito.doNothing().when(timelineProducer).sendEvent(Mockito.any(QueueOperationDTO.class));

    assertEquals(WalletStatus.REFUNDABLE.name(), TEST_WALLET.getStatus());
    assertEquals(TEST_COUNT, TEST_WALLET.getNInstr());
  }

  @Test
  void deleteInstrument_ok_with_instrument() {
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name());
    TEST_WALLET.setNInstr(1);
    TEST_WALLET.setIban(null);

    Mockito.when(
            paymentInstrumentRestConnector.deleteInstrument(Mockito.any(DeactivationBodyDTO.class)))
        .thenReturn(INSTRUMENT_RESPONSE_DTO);

    Mockito.doNothing().when(timelineProducer).sendEvent(Mockito.any(QueueOperationDTO.class));

    try {
      walletService.deleteInstrument(INITIATIVE_ID, USER_ID, HPAN);
    } catch (WalletException e) {
      Assertions.fail();
    }
    assertEquals(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name(), TEST_WALLET.getStatus());
    assertEquals(TEST_COUNT, TEST_WALLET.getNInstr());
  }

  @Test
  void deleteInstrument_ok_with_idemp() {
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name());
    TEST_WALLET.setNInstr(1);
    TEST_WALLET.setIban(null);

    Mockito.when(
            paymentInstrumentRestConnector.deleteInstrument(Mockito.any(DeactivationBodyDTO.class)))
        .thenReturn(INSTRUMENT_RESPONSE_DTO_IDEMP);

    Mockito.doNothing().when(timelineProducer).sendEvent(Mockito.any(QueueOperationDTO.class));

    try {
      walletService.deleteInstrument(INITIATIVE_ID, USER_ID, HPAN);
    } catch (WalletException e) {
      Assertions.fail();
    }
    assertEquals(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name(), TEST_WALLET.getStatus());
    assertEquals(TEST_COUNT_IDEMP, TEST_WALLET.getNInstr());
  }

  @Test
  void deleteInstrument_ko_feignexception() {
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    Request request =
        Request.create(Request.HttpMethod.PUT, "url", new HashMap<>(), null, new RequestTemplate());

    Mockito.doThrow(new FeignException.BadRequest("", request, new byte[0], null))
        .when(paymentInstrumentRestConnector)
        .deleteInstrument(Mockito.any(DeactivationBodyDTO.class));

    try {
      walletService.deleteInstrument(INITIATIVE_ID, USER_ID, HPAN);
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
      walletService.deleteInstrument(INITIATIVE_ID, USER_ID, HPAN);
      Assertions.fail();
    } catch (WalletException e) {
      assertEquals(HttpStatus.NOT_FOUND.value(), e.getCode());
      assertEquals(WalletConstants.ERROR_WALLET_NOT_FOUND, e.getMessage());
    }
  }

  @Test
  void checkInitiative_ok() {
    try {
      walletService.checkInitiative(INITIATIVE_ID);
    } catch (WalletException e) {
      Assertions.fail();
    }
  }

  @Test
  void checkInitiative_ko() {
    try {
      walletService.checkInitiative(INITIATIVE_ID_FAIL);
      Assertions.fail();
    } catch (WalletException e) {
      assertEquals(HttpStatus.FORBIDDEN.value(), e.getCode());
      assertEquals(WalletConstants.ERROR_INITIATIVE_KO, e.getMessage());
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

    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    try {
      walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, DESCRIPTION_OK);
    } catch (WalletException e) {
      Assertions.fail();
    }
    assertEquals(WalletStatus.NOT_REFUNDABLE_ONLY_IBAN.name(), TEST_WALLET.getStatus());
  }

  @Test
  void enrollIban_ok_with_instrument() {
    TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name());
    TEST_WALLET.setNInstr(1);

    Mockito.doNothing().when(ibanProducer).sendIban(Mockito.any(IbanQueueDTO.class));

    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    Mockito.doAnswer(
            invocationOnMock -> {
              TEST_WALLET.setIban(IBAN_OK);
              return null;
            })
        .when(walletRepositoryMock)
        .save(Mockito.any(Wallet.class));
    Mockito.doNothing().when(timelineProducer).sendEvent(Mockito.any(QueueOperationDTO.class));

    walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, DESCRIPTION_OK);

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

    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    Mockito.doAnswer(
            invocationOnMock -> {
              TEST_WALLET.setIban(IBAN_OK);
              return null;
            })
        .when(walletRepositoryMock)
        .save(Mockito.any(Wallet.class));
    walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, DESCRIPTION_OK);

    assertEquals(INITIATIVE_ID, TEST_WALLET.getInitiativeId());
    assertEquals(USER_ID, TEST_WALLET.getUserId());

    assertEquals(WalletStatus.NOT_REFUNDABLE_ONLY_IBAN.name(), TEST_WALLET.getStatus());
  }

  @Test
  void enrollIban_ko_iban_not_italian() {
    TEST_WALLET.setIban(null);
    TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name());

    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));
    try {
      walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_KO_NOT_IT, DESCRIPTION_OK);
      Assertions.fail();
    } catch (UnsupportedCountryException e) {
      assertNotNull(e.getMessage());
    }
  }

  @Test
  void enrollIban_ko_iban_wrong() {
    TEST_WALLET.setIban(null);
    TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name());

    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));
    try {
      walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_WRONG, DESCRIPTION_OK);
      Assertions.fail();
    } catch (IbanFormatException e) {
      assertNotNull(e.getMessage());
    }
  }

  @Test
  void enrollIban_ko_iban_digit_control() {
    TEST_WALLET.setIban(null);
    TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_INSTRUMENT.name());

    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));
    try {
      walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_WRONG_DIGIT, DESCRIPTION_OK);
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
      walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, DESCRIPTION_OK);
      Assertions.fail();
    } catch (WalletException e) {
      assertEquals(HttpStatus.NOT_FOUND.value(), e.getCode());
      assertEquals(WalletConstants.ERROR_WALLET_NOT_FOUND, e.getMessage());
    }
  }

  @Test
  void enrollIban_unsubscribed() {
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET_UNSUBSCRIBED));
    try {
      walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, DESCRIPTION_OK);
      Assertions.fail();
    } catch (WalletException e) {
      assertEquals(HttpStatus.BAD_REQUEST.value(), e.getCode());
      assertEquals(WalletConstants.ERROR_INITIATIVE_UNSUBSCRIBED, e.getMessage());
    }
  }

  @Test
  void enrollIban_ok() {
    Mockito.doNothing().when(ibanProducer).sendIban(Mockito.any(IbanQueueDTO.class));
    TEST_WALLET.setIban(null);
    TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE.name());
    TEST_WALLET.setNInstr(0);

    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    Mockito.doAnswer(
            invocationOnMock -> {
              TEST_WALLET.setIban(IBAN_OK);
              return null;
            })
        .when(walletRepositoryMock)
        .save(Mockito.any(Wallet.class));

    walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, DESCRIPTION_OK);

    assertEquals(WalletStatus.NOT_REFUNDABLE_ONLY_IBAN.name(), TEST_WALLET.getStatus());
  }

  @Test
  void enrollIban_update_ok() {
    Mockito.doNothing().when(ibanProducer).sendIban(Mockito.any(IbanQueueDTO.class));
    TEST_WALLET.setIban(IBAN_OK_OTHER);
    TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE_ONLY_IBAN.name());
    TEST_WALLET.setNInstr(0);

    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    Mockito.doAnswer(
            invocationOnMock -> {
              TEST_WALLET.setIban(IBAN_OK);
              return null;
            })
        .when(walletRepositoryMock)
        .save(Mockito.any(Wallet.class));

    walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, DESCRIPTION_OK);

    assertEquals(WalletStatus.NOT_REFUNDABLE_ONLY_IBAN.name(), TEST_WALLET.getStatus());
  }

  @Test
  void getWalletDetail_ok() {
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));
    TEST_WALLET.setIban(IBAN_OK);
    try {
      InitiativeDTO actual = walletService.getWalletDetail(INITIATIVE_ID, USER_ID);
      assertEquals(INITIATIVE_DTO.getInitiativeId(), actual.getInitiativeId());
      assertEquals(INITIATIVE_DTO.getInitiativeName(), actual.getInitiativeName());
      assertEquals(INITIATIVE_DTO.getStatus(), actual.getStatus());
      assertEquals(INITIATIVE_DTO.getEndDate(), actual.getEndDate());
      assertEquals(INITIATIVE_DTO.getIban(), actual.getIban());
      assertEquals(INITIATIVE_DTO.getNInstr(), actual.getNInstr());
      assertEquals(INITIATIVE_DTO.getAmount(), actual.getAmount());
      assertEquals(INITIATIVE_DTO.getAccrued(), actual.getAccrued());
      assertEquals(INITIATIVE_DTO.getRefunded(), actual.getRefunded());
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
  void getInitiativeList_ok() {
    TEST_WALLET.setIban(IBAN_OK);
    List<Wallet> walletList = new ArrayList<>();
    walletList.add(TEST_WALLET);

    Mockito.when(walletRepositoryMock.findByUserId(USER_ID)).thenReturn(walletList);

    InitiativeListDTO initiativeListDto = walletService.getInitiativeList(USER_ID);

    assertFalse(initiativeListDto.getInitiativeList().isEmpty());

    InitiativeDTO actual = initiativeListDto.getInitiativeList().get(0);
    assertEquals(INITIATIVE_DTO.getInitiativeId(), actual.getInitiativeId());
    assertEquals(INITIATIVE_DTO.getInitiativeName(), actual.getInitiativeName());
    assertEquals(INITIATIVE_DTO.getIban(), actual.getIban());
    assertEquals(INITIATIVE_DTO.getStatus(), actual.getStatus());
  }

  @Test
  void createWallet() {
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
  void deleteOperation_ok() {
    IbanQueueWalletDTO iban =
        new IbanQueueWalletDTO(
            USER_ID, INITIATIVE_ID, IBAN_OK, STATUS_KO, LocalDateTime.now().toString());
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    Mockito.doAnswer(
            invocationOnMock -> {
              TEST_WALLET.setIban(null);
              TEST_WALLET.setStatus(WalletStatus.NOT_REFUNDABLE.name());
              return null;
            })
        .when(walletRepositoryMock)
        .save(Mockito.any(Wallet.class));
    Mockito.doNothing()
        .when(notificationProducer)
        .sendCheckIban(Mockito.any(NotificationQueueDTO.class));
    try {
      walletService.deleteOperation(iban);
      assertNull(TEST_WALLET.getIban());
      assertNotNull(iban.getIban());
      assertEquals(WalletStatus.NOT_REFUNDABLE.name(), TEST_WALLET.getStatus());
      assertEquals(TEST_WALLET.getUserId(), iban.getUserId());
      assertEquals(STATUS_KO, iban.getStatus());
      assertNotNull(iban.getQueueDate());
      assertNotNull(iban);
    } catch (WalletException e) {
      Assertions.fail();
    }
  }

  @Test
  void deleteOperation_ko() {
    IbanQueueWalletDTO iban =
        new IbanQueueWalletDTO(
            USER_ID, INITIATIVE_ID, IBAN_OK, STATUS_KO, LocalDateTime.now().toString());
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.empty());
    try {
      walletService.deleteOperation(iban);
      Assertions.fail();
    } catch (WalletException e) {
      assertEquals(HttpStatus.NOT_FOUND.value(), e.getCode());
      assertEquals(WalletConstants.ERROR_WALLET_NOT_FOUND, e.getMessage());
    }
  }

  @Test
  void unsbubscribe_ok() {

    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    Mockito.doAnswer(
            invocationOnMock -> {
              TEST_WALLET.setUnsubscribeDate(LocalDateTime.now());
              TEST_WALLET.setStatus(WalletStatus.UNSUBSCRIBED);
              return null;
            })
        .when(walletRepositoryMock)
        .save(Mockito.any(Wallet.class));
    try {
      walletService.unsubscribe(INITIATIVE_ID, USER_ID);
      assertNotNull(TEST_WALLET.getUnsubscribeDate());
      assertEquals(WalletStatus.UNSUBSCRIBED, TEST_WALLET.getStatus());
    } catch (WalletException e) {
      Assertions.fail();
    }
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
      assertEquals(HttpStatus.BAD_REQUEST.value(), e.getCode());
    }
  }

  @Test
  void processTransaction_ok() {
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));
    walletService.processTransaction(REWARD_TRX_DTO_REWARDED);
    Mockito.verify(walletRepositoryMock, Mockito.times(1)).save(Mockito.any());
    Mockito.verify(timelineProducer, Mockito.times(1)).sendEvent(Mockito.any());
  }

  @Test
  void processTransaction_not_rewarded() {
    walletService.processTransaction(REWARD_TRX_DTO);
    Mockito.verify(walletRepositoryMock, Mockito.times(0)).save(Mockito.any());
    Mockito.verify(timelineProducer, Mockito.times(0)).sendEvent(Mockito.any());
  }
}
