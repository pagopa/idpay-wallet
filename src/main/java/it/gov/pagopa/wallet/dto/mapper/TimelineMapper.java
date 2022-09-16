package it.gov.pagopa.wallet.dto.mapper;

import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.DeactivationBodyDTO;
import it.gov.pagopa.wallet.dto.EvaluationDTO;
import it.gov.pagopa.wallet.dto.InstrumentCallBodyDTO;
import it.gov.pagopa.wallet.dto.QueueOperationDTO;
import it.gov.pagopa.wallet.dto.RewardTransactionDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class TimelineMapper {

  public QueueOperationDTO transactionToTimeline(
      String initiativeId, RewardTransactionDTO rewardTransaction, BigDecimal accruedReward) {
    return QueueOperationDTO.builder()
        .initiativeId(initiativeId)
        .userId(rewardTransaction.getUserId())
        .operationType("TRANSACTION")
        .operationDate(rewardTransaction.getTrxDate().toLocalDateTime())
        .hpan(rewardTransaction.getHpan())
        .circuitType(rewardTransaction.getCircuitType())
        .amount(rewardTransaction.getAmount())
        .accrued(accruedReward)
        .idTrxIssuer(rewardTransaction.getIdTrxIssuer())
        .idTrxAcquirer(rewardTransaction.getIdTrxAcquirer())
        .build();
  }

  public QueueOperationDTO deleteInstrumentToTimeline(DeactivationBodyDTO dto) {
    return QueueOperationDTO.builder()
        .initiativeId(dto.getInitiativeId())
        .userId(dto.getUserId())
        .channel("APP_IO")
        .hpan(dto.getHpan())
        .operationType("DELETE_INSTRUMENT")
        .operationDate(dto.getDeactivationDate())
        .build();
  }

  public QueueOperationDTO enrollInstrumentToTimeline(InstrumentCallBodyDTO dto) {
    return QueueOperationDTO.builder()
        .initiativeId(dto.getInitiativeId())
        .userId(dto.getUserId())
        .channel(dto.getChannel())
        .hpan(dto.getHpan())
        .operationType("ADD_INSTRUMENT")
        .operationDate(LocalDateTime.now())
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
}
