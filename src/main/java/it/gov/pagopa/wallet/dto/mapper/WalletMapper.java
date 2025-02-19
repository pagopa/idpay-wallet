package it.gov.pagopa.wallet.dto.mapper;

import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.*;
import it.gov.pagopa.wallet.enums.WalletStatus;
import it.gov.pagopa.wallet.model.Wallet;
import it.gov.pagopa.wallet.utils.Utilities;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class WalletMapper {
    private final Utilities utilities;

    public WalletMapper(Utilities utilities) {
        this.utilities = utilities;
    }

    public Wallet map(EvaluationDTO evaluationDTO) {
        return Wallet.builder()
                .id(evaluationDTO.getUserId().concat("_").concat(evaluationDTO.getInitiativeId()))
                .initiativeId(evaluationDTO.getInitiativeId())
                .initiativeName(evaluationDTO.getInitiativeName())
                .endDate(evaluationDTO.getInitiativeEndDate())
                .organizationId(evaluationDTO.getOrganizationId())
                .organizationName(evaluationDTO.getOrganizationName())
                .userId(evaluationDTO.getUserId())
                .familyId(evaluationDTO.getFamilyId())
                .acceptanceDate(evaluationDTO.getAdmissibilityCheckDate())
                .status(WalletStatus.NOT_REFUNDABLE.name())
                .amountCents(evaluationDTO.getBeneficiaryBudgetCents())
                .accruedCents(0L)
                .refundedCents(0L)
                .lastCounterUpdate(LocalDateTime.now())
                .initiativeRewardType(evaluationDTO.getInitiativeRewardType())
                .isLogoPresent(evaluationDTO.getIsLogoPresent())
                .maxTrx(evaluationDTO.getMaxTrx())
                .counterVersion(0L)
                .counterHistory(new ArrayList<>())
                .build();
    }

    public WalletDTO toInitiativeDTO(Wallet wallet) {
        return WalletDTO.builder()
                .familyId(wallet.getFamilyId())
                .initiativeId(wallet.getInitiativeId())
                .initiativeName(wallet.getInitiativeName())
                .endDate(wallet.getEndDate())
                .status(wallet.getStatus())
                .amountCents(wallet.getAmountCents())
                .accruedCents(wallet.getAccruedCents() - wallet.getRefundedCents())
                .refundedCents(wallet.getRefundedCents())
                .nInstr(wallet.getNInstr())
                .iban(wallet.getIban())
                .lastCounterUpdate(wallet.getLastCounterUpdate())
                .initiativeRewardType(wallet.getInitiativeRewardType())
                .logoURL(Boolean.TRUE.equals(wallet.getIsLogoPresent()) ? utilities.createLogoUrl(wallet.getOrganizationId(), wallet.getInitiativeId()) : null)
                .organizationName(wallet.getOrganizationName())
                .nTrx(wallet.getNTrx())
                .maxTrx(wallet.getMaxTrx())
                .serviceId(wallet.getServiceId())
                .build();
    }

    public WalletDTO toIssuerInitiativeDTO(Wallet wallet) {
        return WalletDTO.builder()
                .amountCents(wallet.getAmountCents())
                .accruedCents(wallet.getAccruedCents() -wallet.getRefundedCents())
                .refundedCents(wallet.getRefundedCents())
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
