package it.gov.pagopa.wallet.dto.mapper;

import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.*;
import it.gov.pagopa.wallet.enums.WalletStatus;
import it.gov.pagopa.wallet.model.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import it.gov.pagopa.wallet.utils.Utilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WalletMapper {
    @Autowired
    Utilities utilities;

    public Wallet map(EvaluationDTO evaluationDTO) {
        return Wallet.builder()
                .initiativeId(evaluationDTO.getInitiativeId())
                .initiativeName(evaluationDTO.getInitiativeName())
                .endDate(evaluationDTO.getInitiativeEndDate())
                .organizationId(evaluationDTO.getOrganizationId())
                .organizationName(evaluationDTO.getOrganizationName())
                .userId(evaluationDTO.getUserId())
                .familyId(evaluationDTO.getFamilyId())
                .acceptanceDate(evaluationDTO.getAdmissibilityCheckDate())
                .status(WalletStatus.NOT_REFUNDABLE.name())
                .amount(evaluationDTO.getBeneficiaryBudget())
                .accrued(BigDecimal.valueOf(0.00))
                .refunded(BigDecimal.valueOf(0.00))
                .lastCounterUpdate(LocalDateTime.now())
                .initiativeRewardType(evaluationDTO.getInitiativeRewardType())
                .isLogoPresent(evaluationDTO.getIsLogoPresent())
                .build();
    }

    public WalletDTO toInitiativeDTO(Wallet wallet) {
        return WalletDTO.builder()
                .familyId(wallet.getFamilyId())
                .initiativeId(wallet.getInitiativeId())
                .initiativeName(wallet.getInitiativeName())
                .endDate(wallet.getEndDate())
                .status(wallet.getStatus())
                .amount(wallet.getAmount())
                .accrued(wallet.getAccrued().subtract(wallet.getRefunded()))
                .refunded(wallet.getRefunded())
                .nInstr(wallet.getNInstr())
                .iban(wallet.getIban())
                .lastCounterUpdate(wallet.getLastCounterUpdate())
                .initiativeRewardType(wallet.getInitiativeRewardType())
                .logoURL(Boolean.TRUE.equals(wallet.getIsLogoPresent()) ? utilities.createLogoUrl(wallet.getOrganizationId(), wallet.getInitiativeId()) : null)
                .organizationName(wallet.getOrganizationName())
                .build();
    }

    public WalletDTO toIssuerInitiativeDTO(Wallet wallet) {
        return WalletDTO.builder()
                .amount(wallet.getAmount())
                .accrued(wallet.getAccrued().subtract(wallet.getRefunded()))
                .refunded(wallet.getRefunded())
                .lastCounterUpdate(wallet.getLastCounterUpdate())
                .build();
    }

    public InitiativesStatusDTO toInstrStatusOnInitiativeDTO(WalletDTO wallet) {
        return InitiativesStatusDTO.builder()
                .initiativeId(wallet.getInitiativeId())
                .initiativeName(wallet.getInitiativeName())
                .status(WalletConstants.INSTRUMENT_STATUS_DEFAULT)
                .build();
    }

    public InitiativesWithInstrumentDTO toInstrumentOnInitiativesDTO(String idWallet,
                                                                     InstrumentDetailDTO instrumentDetailDTO,
                                                                     List<InitiativesStatusDTO> initiativesStatusDTO) {
        return InitiativesWithInstrumentDTO.builder()
                .idWallet(idWallet)
                .maskedPan(instrumentDetailDTO.getMaskedPan())
                .brand(instrumentDetailDTO.getBrand())
                .initiativeList(initiativesStatusDTO).build();
    }
}
