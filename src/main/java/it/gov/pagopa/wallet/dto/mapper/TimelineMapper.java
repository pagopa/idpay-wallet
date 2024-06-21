package it.gov.pagopa.wallet.dto.mapper;

import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.*;
import it.gov.pagopa.wallet.enums.ChannelTransaction;
import it.gov.pagopa.wallet.enums.WalletStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class TimelineMapper {

    public QueueOperationDTO transactionToTimeline(
            String initiativeId, RewardTransactionDTO rewardTransaction, Long accruedRewardCents) {
    return QueueOperationDTO.builder()
        .eventId(rewardTransaction.getId())
        .initiativeId(initiativeId)
        .userId(rewardTransaction.getUserId())
        .operationType(
            rewardTransaction.getOperationType().equals("00") ? "TRANSACTION" : "REVERSAL")
        .operationDate(getOperationDate(rewardTransaction))
        .maskedPan(rewardTransaction.getMaskedPan())
        .instrumentId(rewardTransaction.getInstrumentId())
        .brandLogo(rewardTransaction.getBrandLogo())
        .brand(rewardTransaction.getBrand())
        .circuitType(rewardTransaction.getCircuitType())
        .amountCents(rewardTransaction.getAmountCents())
        .effectiveAmountCents(rewardTransaction.getEffectiveAmountCents())
        .accruedCents(accruedRewardCents)
        .idTrxIssuer(rewardTransaction.getIdTrxIssuer())
        .idTrxAcquirer(rewardTransaction.getIdTrxAcquirer())
        .channel(rewardTransaction.getChannel())
        .status(rewardTransaction.getStatus())
        .businessName(rewardTransaction.getBusinessName())
        .build();
    }

    private static LocalDateTime getOperationDate(RewardTransactionDTO rewardTransaction) {
        if("CANCELLED".equals(rewardTransaction.getStatus())) {
            return rewardTransaction.getElaborationDateTime();
        }

        if ("AUTHORIZED".equals(rewardTransaction.getStatus()) || "REWARDED".equals(rewardTransaction.getStatus())
                && ChannelTransaction.isChannelPresent(rewardTransaction.getChannel())) {
            return rewardTransaction.getTrxChargeDate()
                    .atZoneSameInstant(ZoneId.of("Europe/Rome"))
                    .toLocalDateTime();
        }

        return rewardTransaction.getTrxDate()
                .atZoneSameInstant(ZoneId.of("Europe/Rome"))
                .toLocalDateTime();
    }
    public QueueOperationDTO deleteInstrumentToTimeline(
            String initiativeId, String userId, String maskedPan, String brandLogo, String brand) {
        return QueueOperationDTO.builder()
                .initiativeId(initiativeId)
                .userId(userId)
                .channel(WalletConstants.CHANNEL_PM)
                .maskedPan(maskedPan)
                .brandLogo(brandLogo)
                .brand(brand)
                .operationType("DELETE_INSTRUMENT")
                .operationDate(LocalDateTime.now())
                .build();
    }

    public QueueOperationDTO suspendToTimeline(
            String initiativeId, String userId, LocalDateTime localDateTime) {
        return QueueOperationDTO.builder()
                .initiativeId(initiativeId)
                .userId(userId)
                .operationType(WalletStatus.SUSPENDED)
                .operationDate(localDateTime)
                .build();
    }

    public QueueOperationDTO readmitToTimeline(
            String initiativeId, String userId, LocalDateTime localDateTime) {
        return QueueOperationDTO.builder()
                .initiativeId(initiativeId)
                .userId(userId)
                .operationType(WalletConstants.TIMELINE_READMITTED)
                .operationDate(localDateTime)
                .build();
    }

    public QueueOperationDTO ackToTimeline(InstrumentAckDTO dto) {
        return QueueOperationDTO.builder()
                .initiativeId(dto.getInitiativeId())
                .userId(dto.getUserId())
                .channel(dto.getChannel())
                .instrumentType(dto.getInstrumentType())
                .maskedPan(dto.getMaskedPan())
                .brandLogo(dto.getBrandLogo())
                .brand(dto.getBrand())
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

    public QueueOperationDTO ibanToTimeline(String initiativeId, String userId, String iban, String channel) {
        return QueueOperationDTO.builder()
                .initiativeId(initiativeId)
                .userId(userId)
                .channel(channel)
                .iban(iban)
                .operationType("ADD_IBAN")
                .operationDate(LocalDateTime.now())
                .build();
    }

    public QueueOperationDTO refundToTimeline(RefundDTO dto) {
        String operationType = (dto.getStatus().equals("ACCEPTED")) ? "PAID_REFUND" : "REJECTED_REFUND";
        return QueueOperationDTO.builder()
                .eventId(dto.getExternalId())
                .initiativeId(dto.getInitiativeId())
                .userId(dto.getBeneficiaryId())
                .iban(dto.getIban())
                .status(dto.getRewardStatus())
                .refundType(dto.getRefundType())
                .operationType(operationType)
                .operationDate(dto.getFeedbackDate())
                .amountCents(dto.getRewardCents())
                .effectiveAmountCents(dto.getEffectiveRewardCents())
                .rewardNotificationId(dto.getRewardNotificationId())
                .cro(dto.getCro())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .transferDate(dto.getTransferDate())
                .userNotificationDate(dto.getUserNotificationDate())
                .rewardFeedbackProgressive(dto.getFeedbackProgressive())
                .build();
    }
    public QueueOperationDTO unsubscribeToTimeline(
            String initiativeId, String userId, LocalDateTime localDateTime) {
        return QueueOperationDTO.builder()
                .initiativeId(initiativeId)
                .userId(userId)
                .operationType(WalletStatus.UNSUBSCRIBED)
                .operationDate(localDateTime)
                .build();
    }
}
