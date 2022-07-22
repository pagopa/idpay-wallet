package it.gov.pagopa.wallet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.EnrollmentStatusDTO;
import it.gov.pagopa.wallet.dto.EvaluationDTO;
import it.gov.pagopa.wallet.dto.IbanDTO;
import it.gov.pagopa.wallet.dto.IbanQueueDTO;
import it.gov.pagopa.wallet.dto.InitiativeDTO;
import it.gov.pagopa.wallet.dto.InitiativeListDTO;
import it.gov.pagopa.wallet.dto.InstrumentCallBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentResponseDTO;
import it.gov.pagopa.wallet.dto.mapper.WalletMapper;
import it.gov.pagopa.wallet.dto.QueueOperationDTO;
import it.gov.pagopa.wallet.event.IbanProducer;
import it.gov.pagopa.wallet.event.TimelineProducer;
import it.gov.pagopa.wallet.exception.WalletException;
import it.gov.pagopa.wallet.model.Wallet;
import it.gov.pagopa.wallet.repository.WalletRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(value = {WalletService.class})
class WalletServiceTest {

  @MockBean IbanProducer ibanProducer;

  @MockBean TimelineProducer timelineProducer;

  @MockBean WalletRepository walletRepositoryMock;

  @MockBean WalletRestService walletRestServiceMock;

  @MockBean WalletMapper walletMapper;

  @Autowired WalletService walletService;

  private static final String USER_ID = "TEST_USER_ID";
  private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";
  private static final String INITIATIVE_ID_FAIL = "FAIL";
  private static final String HPAN = "TEST_HPAN";
  private static final String IBAN_OK = "IT09P3608105138205493205495";
  private static final String IBAN_OK_OTHER = "IT09P3608105138205493205494";
  private static final String IBAN_KO_NOT_IT = "GB29NWBK60161331926819";
  private static final String IBAN_WRONG = "it99C1234567890123456789012222";
  private static final String IBAN_WRONG_DIGIT = "IT09P3608105138205493205496";
  private static final String DESCRIPTION_OK = "conto cointestato";
  private static final String CHANNEL_OK = "APP_IO";
  private static final String HOLDER_BANK_OK = "Unicredit";
  private static final LocalDateTime TEST_DATE = LocalDateTime.now();
  private static final BigDecimal TEST_AMOUNT = BigDecimal.valueOf(2.00);
  private static final BigDecimal TEST_ACCRUED = BigDecimal.valueOf(0.00);
  private static final BigDecimal TEST_REFUNDED = BigDecimal.valueOf(0.00);
  private static final int TEST_COUNT = 2;

  private static final Wallet TEST_WALLET_INSTRUMENT =
      Wallet.builder()
          .userId(USER_ID)
          .initiativeId(INITIATIVE_ID)
          .initiativeName(INITIATIVE_ID)
          .status(WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_INSTRUMENT)
          .acceptanceDate(TEST_DATE)
          .endDate(TEST_DATE)
          .amount(TEST_AMOUNT)
          .build();

  private static final Wallet TEST_WALLET =
      Wallet.builder()
          .userId(USER_ID)
          .initiativeId(INITIATIVE_ID)
          .initiativeName(INITIATIVE_ID)
          .acceptanceDate(TEST_DATE)
          .status(WalletConstants.STATUS_NOT_REFUNDABLE)
          .endDate(TEST_DATE)
          .amount(TEST_AMOUNT)
          .accrued(TEST_ACCRUED)
          .refunded(TEST_REFUNDED)
          .build();

  private static final InstrumentResponseDTO INSTRUMENT_RESPONSE_DTO =
      new InstrumentResponseDTO(TEST_COUNT);

  private static final InitiativeDTO INITIATIVE_DTO =
      new InitiativeDTO(
          INITIATIVE_ID,
          INITIATIVE_ID,
          WalletConstants.STATUS_NOT_REFUNDABLE,
          IBAN_OK,
          TEST_DATE.toString(),
          "0",
          String.valueOf(TEST_AMOUNT),
          String.valueOf(TEST_ACCRUED),
          String.valueOf(TEST_REFUNDED));

  private static final EvaluationDTO OUTCOME_KO =
      new EvaluationDTO(USER_ID, INITIATIVE_ID, "ONBOARDING_KO", TEST_DATE, null);
  private static final EvaluationDTO OUTCOME_OK =
      new EvaluationDTO(USER_ID, INITIATIVE_ID, "ONBOARDING_OK", TEST_DATE, null);

  static {
    TEST_WALLET.setIban(IBAN_OK);
    TEST_WALLET.setDescription(DESCRIPTION_OK);
  }

  @Test
  void enrollInstrument_ok() throws Exception {
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    TEST_WALLET.setStatus(WalletConstants.STATUS_NOT_REFUNDABLE);

    Mockito.when(
            walletRestServiceMock.callPaymentInstrument(Mockito.any(InstrumentCallBodyDTO.class)))
        .thenReturn(INSTRUMENT_RESPONSE_DTO);

    try {
      walletService.enrollInstrument(INITIATIVE_ID, USER_ID, HPAN);
    } catch (WalletException e) {
      Assertions.fail();
    }
    assertEquals(WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_INSTRUMENT, TEST_WALLET.getStatus());
    assertEquals(TEST_COUNT, TEST_WALLET.getNInstr());
  }

  @Test
  void enrollInstrument_ok_with_iban() throws Exception {
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    TEST_WALLET.setStatus(WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_IBAN);

    Mockito.when(
            walletRestServiceMock.callPaymentInstrument(Mockito.any(InstrumentCallBodyDTO.class)))
        .thenReturn(INSTRUMENT_RESPONSE_DTO);

    try {
      walletService.enrollInstrument(INITIATIVE_ID, USER_ID, HPAN);
    } catch (WalletException e) {
      Assertions.fail();
    }

    Mockito.doNothing().when(timelineProducer).sendEvent(Mockito.any(QueueOperationDTO.class));

    assertEquals(WalletConstants.STATUS_REFUNDABLE, TEST_WALLET.getStatus());
    assertEquals(TEST_COUNT, TEST_WALLET.getNInstr());
  }

  @Test
  void enrollInstrument_ok_with_instrument() throws Exception {
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    TEST_WALLET.setStatus(WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_INSTRUMENT);

    Mockito.when(
            walletRestServiceMock.callPaymentInstrument(Mockito.any(InstrumentCallBodyDTO.class)))
        .thenReturn(INSTRUMENT_RESPONSE_DTO);

    Mockito.doNothing().when(timelineProducer).sendEvent(Mockito.any(QueueOperationDTO.class));

    try {
      walletService.enrollInstrument(INITIATIVE_ID, USER_ID, HPAN);
    } catch (WalletException e) {
      Assertions.fail();
    }
    assertEquals(WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_INSTRUMENT, TEST_WALLET.getStatus());
    assertEquals(TEST_COUNT, TEST_WALLET.getNInstr());
  }

  @Test
  void enrollInstrument_ko_httpclienterrorexception() throws Exception {
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    Mockito.doThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN))
        .when(walletRestServiceMock)
        .callPaymentInstrument(Mockito.any(InstrumentCallBodyDTO.class));

    try {
      walletService.enrollInstrument(INITIATIVE_ID, USER_ID, HPAN);
      Assertions.fail();
    } catch (WalletException e) {
      assertEquals(HttpStatus.FORBIDDEN.value(), e.getCode());
    }
  }

  @Test
  void enrollInstrument_ko_jsonprocessingexception() throws Exception {
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    Mockito.doThrow(new JsonProcessingException("") {})
        .when(walletRestServiceMock)
        .callPaymentInstrument(Mockito.any(InstrumentCallBodyDTO.class));

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
    TEST_WALLET.setStatus(WalletConstants.STATUS_NOT_REFUNDABLE);
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    try {
      walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, DESCRIPTION_OK);
    } catch (WalletException e) {
      Assertions.fail();
    }
    assertEquals(WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_IBAN, TEST_WALLET.getStatus());
  }

  @Test
  void enrollIban_ok_with_instrument() {
    Mockito.doNothing().when(ibanProducer).sendIban(Mockito.any(IbanQueueDTO.class));

    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET_INSTRUMENT));

    Mockito.doAnswer(invocationOnMock -> {
      TEST_WALLET_INSTRUMENT.setIban(IBAN_OK);
      TEST_WALLET_INSTRUMENT.setDescription(DESCRIPTION_OK);
      TEST_WALLET_INSTRUMENT.setChannel(WalletConstants.CHANNEL_APP_IO);
      TEST_WALLET_INSTRUMENT.setHolderBank(WalletConstants.HOLDER_BANK);
      return null;
    }).when(walletRepositoryMock).save(Mockito.any(Wallet.class));
    Mockito.doNothing().when(timelineProducer).sendEvent(Mockito.any(QueueOperationDTO.class));

    walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, DESCRIPTION_OK);

    assertEquals(INITIATIVE_ID, TEST_WALLET_INSTRUMENT.getInitiativeId());
    assertEquals(USER_ID, TEST_WALLET_INSTRUMENT.getUserId());
    assertEquals(CHANNEL_OK, TEST_WALLET_INSTRUMENT.getChannel());
    assertEquals(IBAN_OK, TEST_WALLET_INSTRUMENT.getIban());
    assertEquals(HOLDER_BANK_OK, TEST_WALLET_INSTRUMENT.getHolderBank());
    assertEquals(DESCRIPTION_OK, TEST_WALLET_INSTRUMENT.getDescription());

    assertEquals(WalletConstants.STATUS_REFUNDABLE, TEST_WALLET_INSTRUMENT.getStatus());
  }

  @Test
  void enrollIban_ok_idemp() {
    Mockito.doNothing().when(ibanProducer).sendIban(Mockito.any(IbanQueueDTO.class));
    TEST_WALLET.setIban(IBAN_OK);
    TEST_WALLET.setStatus(WalletConstants.STATUS_REFUNDABLE);
    TEST_WALLET.setDescription(DESCRIPTION_OK);

    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    Mockito.doAnswer(
            invocationOnMock -> {
              TEST_WALLET.setIban(IBAN_OK);
              TEST_WALLET.setDescription(DESCRIPTION_OK);
              TEST_WALLET.setChannel(WalletConstants.CHANNEL_APP_IO);
              TEST_WALLET.setHolderBank(WalletConstants.HOLDER_BANK);
              return null;
            })
        .when(walletRepositoryMock)
        .save(Mockito.any(Wallet.class));
    walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, DESCRIPTION_OK);

    assertEquals(INITIATIVE_ID, TEST_WALLET.getInitiativeId());
    assertEquals(USER_ID, TEST_WALLET.getUserId());
    assertEquals(CHANNEL_OK, TEST_WALLET.getChannel());
    assertEquals(IBAN_OK, TEST_WALLET.getIban());
    assertEquals(HOLDER_BANK_OK, TEST_WALLET.getHolderBank());
    assertEquals(DESCRIPTION_OK, TEST_WALLET.getDescription());

    assertEquals(WalletConstants.STATUS_REFUNDABLE, TEST_WALLET.getStatus());
  }

  @Test
  void enrollIban_ko_iban_not_italian() {
    TEST_WALLET.setIban(IBAN_KO_NOT_IT);
    TEST_WALLET.setDescription(DESCRIPTION_OK);
    TEST_WALLET.setStatus(WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_INSTRUMENT);
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
    TEST_WALLET.setIban(IBAN_WRONG);
    TEST_WALLET.setDescription(DESCRIPTION_OK);
    TEST_WALLET.setStatus(WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_INSTRUMENT);
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
    TEST_WALLET.setIban(IBAN_WRONG_DIGIT);
    TEST_WALLET.setDescription(DESCRIPTION_OK);
    TEST_WALLET.setStatus(WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_INSTRUMENT);
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
  void enrollIban_ok() {
    Mockito.doNothing().when(ibanProducer).sendIban(Mockito.any(IbanQueueDTO.class));
    TEST_WALLET.setIban(null);
    TEST_WALLET.setStatus(WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_IBAN);
    TEST_WALLET.setDescription(DESCRIPTION_OK);
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    Mockito.doAnswer(
            invocationOnMock -> {
              TEST_WALLET.setIban(IBAN_OK);
              TEST_WALLET.setDescription(DESCRIPTION_OK);
              TEST_WALLET.setChannel(WalletConstants.CHANNEL_APP_IO);
              TEST_WALLET.setHolderBank(WalletConstants.HOLDER_BANK);
              return null;
            })
        .when(walletRepositoryMock)
        .save(Mockito.any(Wallet.class));

    walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, DESCRIPTION_OK);

    assertEquals(WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_IBAN, TEST_WALLET.getStatus());
  }

  @Test
  void enrollIban_update_ok() {
    Mockito.doNothing().when(ibanProducer).sendIban(Mockito.any(IbanQueueDTO.class));
    TEST_WALLET.setIban(IBAN_OK_OTHER);
    TEST_WALLET.setDescription(DESCRIPTION_OK);
    TEST_WALLET.setStatus(WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_IBAN);
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    Mockito.doAnswer(
            invocationOnMock -> {
              TEST_WALLET.setIban(IBAN_OK);
              TEST_WALLET.setDescription(DESCRIPTION_OK);
              TEST_WALLET.setChannel(WalletConstants.CHANNEL_APP_IO);
              TEST_WALLET.setHolderBank(WalletConstants.HOLDER_BANK);
              return null;
            })
        .when(walletRepositoryMock)
        .save(Mockito.any(Wallet.class));

    walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, DESCRIPTION_OK);

    assertEquals(WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_IBAN, TEST_WALLET.getStatus());
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
  void getIban_ok() {

    IbanDTO ibanDTO = new IbanDTO(IBAN_OK, DESCRIPTION_OK, HOLDER_BANK_OK, CHANNEL_OK);

    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));
    walletService.getIban(INITIATIVE_ID, USER_ID);

    assertEquals(ibanDTO.getIban(), TEST_WALLET.getIban());
  }

  @Test
  void getIban_ko() {
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.empty());
    try {
      walletService.getIban(INITIATIVE_ID, USER_ID);
    } catch (WalletException e) {
      assertEquals(HttpStatus.NOT_FOUND.value(), e.getCode());
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
    Mockito.verify(timelineProducer, Mockito.times(1)).sendTimelineEvent(Mockito.any());
  }

  @Test
  void createWallet_doNoting() {
    walletService.createWallet(OUTCOME_KO);
    Mockito.verify(walletRepositoryMock, Mockito.times(0)).save(Mockito.any());
    Mockito.verify(timelineProducer, Mockito.times(0)).sendTimelineEvent(Mockito.any());
  }
}
