package it.gov.pagopa.wallet.dto.mapper;

import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.EvaluationDTO;
import it.gov.pagopa.wallet.dto.InstrumentAckDTO;
import it.gov.pagopa.wallet.dto.QueueOperationDTO;
import it.gov.pagopa.wallet.dto.RefundDTO;
import it.gov.pagopa.wallet.dto.RewardTransactionDTO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class TimelineMapper {
  private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100L);

  public QueueOperationDTO transactionToTimeline(
      String initiativeId, RewardTransactionDTO rewardTransaction, BigDecimal accruedReward) {
    return QueueOperationDTO.builder()
        .initiativeId(initiativeId)
        .userId(rewardTransaction.getUserId())
        .operationType(
            rewardTransaction.getOperationType().equals("00") ? "TRANSACTION" : "REVERSAL")
        .operationDate(rewardTransaction.getTrxDate().toLocalDateTime())
        .maskedPan(rewardTransaction.getMaskedPan())
        .instrumentId(rewardTransaction.getInstrumentId())
        .brandLogo(rewardTransaction.getBrandLogo())
        .circuitType(rewardTransaction.getCircuitType())
        .amount(rewardTransaction.getAmount())
        .effectiveAmount(rewardTransaction.getEffectiveAmount())
        .accrued(accruedReward)
        .idTrxIssuer(rewardTransaction.getIdTrxIssuer())
        .idTrxAcquirer(rewardTransaction.getIdTrxAcquirer())
        .build();
  }

  public QueueOperationDTO deleteInstrumentToTimeline(
      String initiativeId, String userId, String maskedPan, String brandLogo) {
    return QueueOperationDTO.builder()
        .initiativeId(initiativeId)
        .userId(userId)
        .channel(WalletConstants.CHANNEL_PM)
        .maskedPan(maskedPan)
        .brandLogo(brandLogo)
        .operationType("DELETE_INSTRUMENT")
        .operationDate(LocalDateTime.now())
        .build();
  }

  public QueueOperationDTO ackToTimeline(InstrumentAckDTO dto) {
    return QueueOperationDTO.builder()
        .initiativeId(dto.getInitiativeId())
        .userId(dto.getUserId())
        .channel(dto.getChannel())
        .maskedPan(dto.getMaskedPan())
        .brandLogo(dto.getBrandLogo())
        .operationType(dto.getOperationType())
        .operationDate(dto.getOperationDate())
        .build();
  }

  public QueueOperationDTO onboardingToTimeline(EvaluationDTO evaluationDTO) {
    return QueueOperationDTO.builder()
        .initiativeId(evaluationDTO.getInitiativeId())
        .userId(evaluationDTO.getUserId())
        .operationType(WalletConstants.ONBOARDING_OPERATION)
        .operationDate(evaluationDTO.getAdmissibilityCheckDate())
        .build();
  }

  public QueueOperationDTO ibanToTimeline(String initiativeId, String userId, String iban) {
    return QueueOperationDTO.builder()
        .initiativeId(initiativeId)
        .userId(userId)
        .channel(WalletConstants.CHANNEL_APP_IO)
        .iban(iban)
        .operationType("ADD_IBAN")
        .operationDate(LocalDateTime.now())
        .build();
  }

  public QueueOperationDTO refundToTimeline(RefundDTO dto) {
    String operationType = (dto.getStatus().equals("ACCEPTED")) ? "PAID_REFUND" : "REJECTED_REFUND";
    return QueueOperationDTO.builder()
        .initiativeId(dto.getInitiativeId())
        .userId(dto.getUserId())
        .operationType(operationType)
        .operationDate(dto.getFeedbackDate())
        .amount(BigDecimal.valueOf(dto.getRewardCents())
            .divide(ONE_HUNDRED, 2, RoundingMode.HALF_DOWN))
        .effectiveAmount(BigDecimal.valueOf(dto.getEffectiveRewardCents())
            .divide(ONE_HUNDRED, 2, RoundingMode.HALF_DOWN))
        .rewardNotificationId(dto.getRewardNotificationId())
        .cro(dto.getCro())
        .rewardFeedbackProgressive(dto.getFeedbackProgressive())
        .build();
  }
}
