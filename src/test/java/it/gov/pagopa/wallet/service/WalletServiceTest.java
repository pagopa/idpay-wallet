package it.gov.pagopa.wallet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.EnrollmentStatusDTO;
import it.gov.pagopa.wallet.dto.IbanCallBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentCallBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentResponseDTO;
import it.gov.pagopa.wallet.exception.WalletException;
import it.gov.pagopa.wallet.model.Wallet;
import it.gov.pagopa.wallet.repository.WalletRepository;
import java.time.LocalDateTime;
import java.util.Optional;
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
  @MockBean WalletRepository walletRepositoryMock;

  @MockBean WalletRestService walletRestServiceMock;

  @Autowired WalletService walletService;

  private static final String USER_ID = "TEST_USER_ID";
  private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";
  private static final String INITIATIVE_ID_FAIL = "FAIL";
  private static final String HPAN = "TEST_HPAN";
  private static final String IBAN_OK = "it99C1234567890123456789012";
  private static final String DESCRIPTION_OK = "conto cointestato";
  private static final LocalDateTime TEST_DATE = LocalDateTime.now();
  private static final String TEST_AMOUNT = "2.00";
  private static final int TEST_COUNT = 2;
  private static final Wallet TEST_WALLET =
      new Wallet(
          USER_ID, INITIATIVE_ID, WalletConstants.STATUS_NOT_REFUNDABLE, TEST_DATE, TEST_AMOUNT);
  private static final InstrumentResponseDTO INSTRUMENT_RESPONSE_DTO =
      new InstrumentResponseDTO(TEST_COUNT);

  @Test
  void enrollInstrument_ok() throws Exception{
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

    try {
      walletService.enrollInstrument(INITIATIVE_ID, USER_ID, HPAN);
    } catch (WalletException e) {
      Assertions.fail();
    }
    assertEquals(
        WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_INSTRUMENT,
        TEST_WALLET.getStatus());
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

    Mockito.doThrow(new JsonProcessingException(""){})
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
      assertEquals(WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_IBAN, actual.getStatus());
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
  void enrollIban_ko_httpclienterrorexception() throws Exception {
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    Mockito.doThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN))
        .when(walletRestServiceMock)
        .callIban(Mockito.any(IbanCallBodyDTO.class));

    try {
      walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK, DESCRIPTION_OK);
      Assertions.fail();
    } catch (WalletException e) {
      assertEquals(HttpStatus.FORBIDDEN.value(), e.getCode());
    }
  }

  @Test
  void enrollIban_ko_jsonprocessingexception() throws Exception {
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    Mockito.doThrow(new JsonProcessingException(""){})
        .when(walletRestServiceMock)
        .callIban(Mockito.any(IbanCallBodyDTO.class));

    try {
      walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK,DESCRIPTION_OK);
      Assertions.fail();
    } catch (WalletException e) {
      assertEquals(HttpStatus.BAD_REQUEST.value(), e.getCode());
    }
  }

  @Test
  void enrollIban_ok_with_instrument() throws Exception {
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    TEST_WALLET.setStatus(WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_INSTRUMENT);

    Mockito.doNothing().when(
        walletRestServiceMock).callIban(Mockito.any(IbanCallBodyDTO.class));

    try {
      walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK,DESCRIPTION_OK);
    } catch (WalletException e) {
      Assertions.fail();
    }
    assertEquals(
        WalletConstants.STATUS_REFUNDABLE,
        TEST_WALLET.getStatus());
  }

  @Test
  void enrollIban_ok_with_iban() throws Exception {
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    TEST_WALLET.setStatus(WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_IBAN);

    Mockito.doNothing().when(
        walletRestServiceMock).callIban(Mockito.any(IbanCallBodyDTO.class));

    try {
      walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK,DESCRIPTION_OK);
    } catch (WalletException e) {
      Assertions.fail();
    }
    assertEquals(
        WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_IBAN,
        TEST_WALLET.getStatus());
  }

  @Test
  void enrollIban_ok() throws Exception {
    Mockito.when(walletRepositoryMock.findByInitiativeIdAndUserId(INITIATIVE_ID, USER_ID))
        .thenReturn(Optional.of(TEST_WALLET));

    TEST_WALLET.setStatus(WalletConstants.STATUS_NOT_REFUNDABLE);

    Mockito.doNothing().when(
        walletRestServiceMock).callIban(Mockito.any(IbanCallBodyDTO.class));

    try {
      walletService.enrollIban(INITIATIVE_ID, USER_ID, IBAN_OK,DESCRIPTION_OK);
    } catch (WalletException e) {
      Assertions.fail();
    }
    assertEquals(
        WalletConstants.STATUS_NOT_REFUNDABLE_ONLY_IBAN,
        TEST_WALLET.getStatus());
  }
}
