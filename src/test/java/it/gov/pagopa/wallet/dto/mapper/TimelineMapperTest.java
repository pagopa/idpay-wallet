package it.gov.pagopa.wallet.dto.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.EvaluationDTO;
import it.gov.pagopa.wallet.dto.InstrumentAckDTO;
import it.gov.pagopa.wallet.dto.QueueOperationDTO;
import it.gov.pagopa.wallet.dto.RefundDTO;
import it.gov.pagopa.wallet.dto.RewardTransactionDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import it.gov.pagopa.wallet.enums.WalletStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TimelineMapper.class)
class TimelineMapperTest {
    private static final String REFUND_ID = "test_refund_id";
    private static final String REFUND_EXTERNAL_ID = "test_refund_external";
    private static final String USER_ID = "test_user";
    private static final String FAMILY_ID = "test_family";
    private static final String INITIATIVE_ID = "test_initiative";
    private static final String ORGANIZATION_ID = "organization_id";
    private static final String REWARD_NOTIFICATION_ID = "reward_notification_id";
    private static final String IBAN = "test_iban";
    private static final String REWARD_STATUS = "reward_status";
    private static final String REFUND_TYPE = "refund_type";
    private static final String CHANNEL = "APP_IO";
    private static final String CRO = "cro";
    private static final String MASKED_PAN = "masked_pan";
    private static final String BRAND_LOGO = "brand_logo";
    private static final String INSTRUMENT_ID = "instrument_id";
    private static final String CIRCUIT_TYPE = "test_circuit";
    private static final String ORGANIZATION_NAME = "TEST_ORGANIZATION_NAME";
    private static final BigDecimal BIG_DECIMAL = BigDecimal.valueOf(0.00);
    private static final LocalDateTime OPERATION_DATE = LocalDateTime.now();
    private static final LocalDate OPERATION_DATE_ONLY_DATE = LocalDate.now();
    private static final LocalDate START_DATE = LocalDate.now();
    private static final LocalDate END_DATE = LocalDate.now().plusDays(2);
    private static final LocalDate TRANSFER_DATE = LocalDate.now();
    private static final LocalDate NOTIFICATION_DATE = LocalDate.now();
    private static final EvaluationDTO EVALUATION_DTO =
            new EvaluationDTO(
                    USER_ID,
                    FAMILY_ID,
                    INITIATIVE_ID,
                    INITIATIVE_ID,
                    OPERATION_DATE_ONLY_DATE,
                    INITIATIVE_ID,
                    WalletConstants.STATUS_ONBOARDING_OK,
                    OPERATION_DATE,
                    OPERATION_DATE,
                    List.of(),
                    new BigDecimal(500),
                    WalletConstants.INITIATIVE_REWARD_TYPE_REFUND,
                    ORGANIZATION_NAME,
                    Boolean.FALSE);
    private static final InstrumentAckDTO INSTRUMENT_ACK_DTO =
            new InstrumentAckDTO(
                    INITIATIVE_ID,
                    USER_ID,
                    WalletConstants.CHANNEL_APP_IO,
                    BRAND_LOGO,
                    BRAND_LOGO,
                    MASKED_PAN,
                    "ADD_INSTRUMENT",
                    OPERATION_DATE,
                    1);

    private static final RewardTransactionDTO REWARD_TRX_DTO_REWARDED =
            RewardTransactionDTO.builder()
                    .userId(USER_ID)
                    .status("REWARDED")
                    .operationType("00")
                    .trxDate(OffsetDateTime.now())
                    .instrumentId(INSTRUMENT_ID)
                    .maskedPan(MASKED_PAN)
                    .brandLogo(BRAND_LOGO)
                    .brand(BRAND_LOGO)
                    .circuitType(CIRCUIT_TYPE)
                    .amount(BIG_DECIMAL)
                    .idTrxIssuer(USER_ID)
                    .idTrxAcquirer(USER_ID)
                    .build();

    private static final RewardTransactionDTO REWARD_TRX_DTO_REVERSAL =
            RewardTransactionDTO.builder()
                    .userId(USER_ID)
                    .status("REWARDED")
                    .operationType("01")
                    .trxDate(OffsetDateTime.now())
                    .instrumentId(INSTRUMENT_ID)
                    .maskedPan(MASKED_PAN)
                    .brandLogo(BRAND_LOGO)
                    .brand(BRAND_LOGO)
                    .circuitType(CIRCUIT_TYPE)
                    .amount(BIG_DECIMAL)
                    .idTrxIssuer(USER_ID)
                    .idTrxAcquirer(USER_ID)
                    .build();

    private static final RefundDTO REFUND_DTO =
            new RefundDTO(
                    REFUND_ID,
                    REFUND_EXTERNAL_ID,
                    REWARD_NOTIFICATION_ID,
                    INITIATIVE_ID,
                    USER_ID,
                    ORGANIZATION_ID,
                    IBAN,
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
                    CRO);

    private static final RefundDTO REFUND_DTO_REJECTED =
            new RefundDTO(
                    REFUND_ID,
                    REFUND_EXTERNAL_ID,
                    REWARD_NOTIFICATION_ID,
                    INITIATIVE_ID,
                    USER_ID,
                    ORGANIZATION_ID,
                    IBAN,
                    "REJECTED",
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
                    CRO);

    @Autowired
    TimelineMapper timelineMapper;

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
        QueueOperationDTO actual = timelineMapper.ibanToTimeline(INITIATIVE_ID, USER_ID, IBAN, CHANNEL);
        assertEquals(USER_ID, actual.getUserId());
        assertEquals(INITIATIVE_ID, actual.getInitiativeId());
        assertEquals("ADD_IBAN", actual.getOperationType());
        assertEquals(WalletConstants.CHANNEL_APP_IO, actual.getChannel());
        assertEquals(IBAN, actual.getIban());
    }

    @Test
    void ackToTimeline() {
        QueueOperationDTO actual = timelineMapper.ackToTimeline(INSTRUMENT_ACK_DTO);
        assertEquals(USER_ID, actual.getUserId());
        assertEquals(INITIATIVE_ID, actual.getInitiativeId());
        assertEquals("ADD_INSTRUMENT", actual.getOperationType());
        assertEquals(WalletConstants.CHANNEL_APP_IO, actual.getChannel());
        assertEquals(MASKED_PAN, actual.getMaskedPan());
        assertEquals(BRAND_LOGO, actual.getBrandLogo());
    }

    @Test
    void deleteInstrumentToTimeline() {
        QueueOperationDTO actual =
                timelineMapper.deleteInstrumentToTimeline(INITIATIVE_ID, USER_ID, MASKED_PAN, BRAND_LOGO, BRAND_LOGO);
        assertEquals(USER_ID, actual.getUserId());
        assertEquals(INITIATIVE_ID, actual.getInitiativeId());
        assertEquals("DELETE_INSTRUMENT", actual.getOperationType());
        assertEquals(MASKED_PAN, actual.getMaskedPan());
        assertEquals(BRAND_LOGO, actual.getBrandLogo());
    }

    @Test
    void transactionToTimeline() {
        QueueOperationDTO actual =
                timelineMapper.transactionToTimeline(INITIATIVE_ID, REWARD_TRX_DTO_REWARDED, BIG_DECIMAL);
        assertEquals(USER_ID, actual.getUserId());
        assertEquals(INITIATIVE_ID, actual.getInitiativeId());
        assertEquals("TRANSACTION", actual.getOperationType());
        assertEquals(MASKED_PAN, actual.getMaskedPan());
        assertEquals(INSTRUMENT_ID, actual.getInstrumentId());
        assertEquals(BRAND_LOGO, actual.getBrandLogo());
        assertEquals(CIRCUIT_TYPE, actual.getCircuitType());
        assertEquals(BIG_DECIMAL, actual.getAmount());
        assertEquals(BIG_DECIMAL, actual.getAccrued());
        assertEquals(USER_ID, actual.getIdTrxIssuer());
        assertEquals(USER_ID, actual.getIdTrxAcquirer());
    }

    @Test
    void transactionToTimelineReversal() {
        QueueOperationDTO actual =
                timelineMapper.transactionToTimeline(INITIATIVE_ID, REWARD_TRX_DTO_REVERSAL, BIG_DECIMAL);
        assertEquals(USER_ID, actual.getUserId());
        assertEquals(INITIATIVE_ID, actual.getInitiativeId());
        assertEquals("REVERSAL", actual.getOperationType());
        assertEquals(MASKED_PAN, actual.getMaskedPan());
        assertEquals(INSTRUMENT_ID, actual.getInstrumentId());
        assertEquals(BRAND_LOGO, actual.getBrandLogo());
        assertEquals(CIRCUIT_TYPE, actual.getCircuitType());
        assertEquals(BIG_DECIMAL, actual.getAmount());
        assertEquals(BIG_DECIMAL, actual.getAccrued());
        assertEquals(USER_ID, actual.getIdTrxIssuer());
        assertEquals(USER_ID, actual.getIdTrxAcquirer());
    }

    @Test
    void refundToTimelineAccepted() {
        QueueOperationDTO actual = timelineMapper.refundToTimeline(REFUND_DTO);
        assertEquals(INITIATIVE_ID, actual.getInitiativeId());
        assertEquals(USER_ID, actual.getUserId());
        assertEquals(REWARD_NOTIFICATION_ID, actual.getRewardNotificationId());
        assertEquals(CRO, actual.getCro());
        assertEquals("PAID_REFUND", actual.getOperationType());
        assertEquals(IBAN, actual.getIban());
        assertEquals(REWARD_STATUS, actual.getStatus());
        assertEquals(REFUND_TYPE, actual.getRefundType());
        assertEquals(START_DATE, actual.getStartDate());
        assertEquals(END_DATE, actual.getEndDate());
        assertEquals(TRANSFER_DATE, actual.getTransferDate());
        assertEquals(NOTIFICATION_DATE, actual.getUserNotificationDate());
    }

    @Test
    void refundToTimelineRejected() {
        QueueOperationDTO actual = timelineMapper.refundToTimeline(REFUND_DTO_REJECTED);
        assertEquals(INITIATIVE_ID, actual.getInitiativeId());
        assertEquals(USER_ID, actual.getUserId());
        assertEquals(REWARD_NOTIFICATION_ID, actual.getRewardNotificationId());
        assertEquals(CRO, actual.getCro());
        assertEquals("REJECTED_REFUND", actual.getOperationType());
        assertEquals(IBAN, actual.getIban());
        assertEquals(REWARD_STATUS, actual.getStatus());
        assertEquals(REFUND_TYPE, actual.getRefundType());
        assertEquals(START_DATE, actual.getStartDate());
        assertEquals(END_DATE, actual.getEndDate());
        assertEquals(TRANSFER_DATE, actual.getTransferDate());
        assertEquals(NOTIFICATION_DATE, actual.getUserNotificationDate());
    }

    @Test
    void suspendToTimeline() {
        QueueOperationDTO actual =
                timelineMapper.suspendToTimeline(INITIATIVE_ID, USER_ID, OPERATION_DATE);
        assertEquals(USER_ID, actual.getUserId());
        assertEquals(WalletStatus.SUSPENDED, actual.getOperationType());
        assertEquals(INITIATIVE_ID, actual.getInitiativeId());
        assertEquals(OPERATION_DATE, actual.getOperationDate());
    }

    @Test
    void readmitToTimeline() {
        QueueOperationDTO actual =
                timelineMapper.readmitToTimeline(INITIATIVE_ID, USER_ID, OPERATION_DATE);
        assertEquals(USER_ID, actual.getUserId());
        assertEquals(WalletConstants.TIMELINE_READMITTED, actual.getOperationType());
        assertEquals(INITIATIVE_ID, actual.getInitiativeId());
        assertEquals(OPERATION_DATE, actual.getOperationDate());
    }

}
