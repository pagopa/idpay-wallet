package it.gov.pagopa.wallet.dto.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.DeactivationBodyDTO;
import it.gov.pagopa.wallet.dto.EvaluationDTO;
import it.gov.pagopa.wallet.dto.InstrumentCallBodyDTO;
import it.gov.pagopa.wallet.dto.QueueOperationDTO;
import it.gov.pagopa.wallet.dto.RewardTransactionDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TimelineMapper.class)
class TimelineMapperTest {
  private static final String USER_ID = "test_user";
  private static final String INITIATIVE_ID = "test_initiative";
  private static final String IBAN = "test_iban";
  private static final String HPAN = "test_hpan";
  private static final String CIRCUIT_TYPE = "test_circuit";
  private static final BigDecimal BIG_DECIMAL = BigDecimal.valueOf(0.00);
  private static final LocalDateTime OPERATION_DATE = LocalDateTime.now();
  private static final EvaluationDTO EVALUATION_DTO =
      new EvaluationDTO(
          USER_ID, INITIATIVE_ID, WalletConstants.STATUS_ONBOARDING_OK, OPERATION_DATE, null);
  private static final InstrumentCallBodyDTO INSTRUMENT_BODY_DTO =
      new InstrumentCallBodyDTO(
          USER_ID, INITIATIVE_ID, HPAN, WalletConstants.CHANNEL_APP_IO, OPERATION_DATE);
  private static final DeactivationBodyDTO DELETE_INSTRUMENT_BODY_DTO =
      new DeactivationBodyDTO(USER_ID, INITIATIVE_ID, HPAN, OPERATION_DATE);

  private static final RewardTransactionDTO REWARD_TRX_DTO_REWARDED =
      RewardTransactionDTO.builder()
          .userId(USER_ID)
          .status("REWARDED")
          .trxDate(OffsetDateTime.now())
          .hpan(HPAN)
          .circuitType(CIRCUIT_TYPE)
          .amount(BIG_DECIMAL)
          .idTrxIssuer(USER_ID)
          .idTrxAcquirer(USER_ID)
          .build();

  @Autowired TimelineMapper timelineMapper;

  @Test
  void onboardingToTimeline() {
    QueueOperationDTO actual = timelineMapper.onboardingToTimeline(EVALUATION_DTO);
    assertEquals(USER_ID, actual.getUserId());
    assertEquals(INITIATIVE_ID, actual.getInitiativeId());
    assertEquals(OPERATION_DATE, actual.getOperationDate());
    assertEquals("ONBOARDING", actual.getOperationType());
  }

  @Test
  void ibanToTimeline() {
    QueueOperationDTO actual = timelineMapper.ibanToTimeline(INITIATIVE_ID, USER_ID, IBAN);
    assertEquals(USER_ID, actual.getUserId());
    assertEquals(INITIATIVE_ID, actual.getInitiativeId());
    assertEquals("ADD_IBAN", actual.getOperationType());
    assertEquals(WalletConstants.CHANNEL_APP_IO, actual.getChannel());
    assertEquals(IBAN, actual.getIban());
  }

  @Test
  void enrollInstrumentToTimeline() {
    QueueOperationDTO actual = timelineMapper.enrollInstrumentToTimeline(INSTRUMENT_BODY_DTO);
    assertEquals(USER_ID, actual.getUserId());
    assertEquals(INITIATIVE_ID, actual.getInitiativeId());
    assertEquals("ADD_INSTRUMENT", actual.getOperationType());
    assertEquals(WalletConstants.CHANNEL_APP_IO, actual.getChannel());
    assertEquals(HPAN, actual.getHpan());
  }

  @Test
  void deleteInstrumentToTimeline() {
    QueueOperationDTO actual =
        timelineMapper.deleteInstrumentToTimeline(DELETE_INSTRUMENT_BODY_DTO);
    assertEquals(USER_ID, actual.getUserId());
    assertEquals(INITIATIVE_ID, actual.getInitiativeId());
    assertEquals("DELETE_INSTRUMENT", actual.getOperationType());
    assertEquals(HPAN, actual.getHpan());
  }

  @Test
  void transactionToTimeline() {
    QueueOperationDTO actual =
        timelineMapper.transactionToTimeline(INITIATIVE_ID, REWARD_TRX_DTO_REWARDED, BIG_DECIMAL);
    assertEquals(USER_ID, actual.getUserId());
    assertEquals(INITIATIVE_ID, actual.getInitiativeId());
    assertEquals("TRANSACTION", actual.getOperationType());
    assertEquals(HPAN, actual.getHpan());
    assertEquals(CIRCUIT_TYPE, actual.getCircuitType());
    assertEquals(BIG_DECIMAL, actual.getAmount());
    assertEquals(BIG_DECIMAL, actual.getAccrued());
    assertEquals(USER_ID, actual.getIdTrxIssuer());
    assertEquals(USER_ID, actual.getIdTrxAcquirer());
  }
}