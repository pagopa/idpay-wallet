package it.gov.pagopa.wallet.dto.mapper;

import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.*;
import it.gov.pagopa.wallet.enums.VoucherStatus;
import it.gov.pagopa.wallet.enums.WalletStatus;
import it.gov.pagopa.wallet.model.Wallet;
import it.gov.pagopa.wallet.utils.Utilities;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class WalletMapper {
    @Value("${app.wallet.expiringDay}")
    private int expiringDay;
    private final Utilities utilities;

    public WalletMapper(Utilities utilities) {
        this.utilities = utilities;
    }

    public Wallet map(EvaluationDTO evaluationDTO) {
        return Wallet.builder()
                .id(evaluationDTO.getUserId().concat("_").concat(evaluationDTO.getInitiativeId()))
                .initiativeId(evaluationDTO.getInitiativeId())
                .initiativeName(evaluationDTO.getInitiativeName())
                .initiativeEndDate(evaluationDTO.getInitiativeEndDate())
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
                .initiativeEndDate(wallet.getInitiativeEndDate())
                .voucherStartDate(wallet.getVoucherStartDate())
                .voucherEndDate(wallet.getVoucherEndDate())
                .status(wallet.getStatus())
                .voucherStatus(setVoucherStatus(wallet))
                .amountCents(wallet.getAmountCents())
                .accruedCents(wallet.getAccruedCents() - wallet.getRefundedCents())
                .refundedCents(wallet.getRefundedCents())
                .nInstr(wallet.getNInstr())
                .iban(wallet.getIban())
                .initiativeRewardType(wallet.getInitiativeRewardType())
                .logoURL(Boolean.TRUE.equals(wallet.getIsLogoPresent()) ? utilities.createLogoUrl(wallet.getOrganizationId(), wallet.getInitiativeId()) : null)
                .organizationName(wallet.getOrganizationName())
                .nTrx(wallet.getNTrx())
                .maxTrx(wallet.getMaxTrx())
                .serviceId(wallet.getServiceId())
                .userMail(wallet.getUserMail())
                .channel(wallet.getChannel())
                .name(wallet.getName())
                .surname(wallet.getSurname())
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

    private String setVoucherStatus(Wallet wallet) {

        if(wallet.getVoucherStartDate() == null || wallet.getVoucherEndDate() == null){
            return null;
        }

        LocalDate today         = LocalDate.now();
        LocalDate start         = wallet.getVoucherStartDate();
        LocalDate end           = wallet.getVoucherEndDate();
        LocalDate expiringFrom  = end.minusDays(expiringDay);

        boolean isAccruedCentsZero      = wallet.getAccruedCents() == 0;
        boolean todayIsBetweenInclusive = (today.isAfter(start) || today.isEqual(start)) && !today.isAfter(end);
        boolean todayInExpiringWindow   = todayIsBetweenInclusive && (today.isEqual(expiringFrom) || today.isAfter(expiringFrom));

        if (wallet.getAccruedCents() > 0) {
            return VoucherStatus.USED.name();
        } else if (today.isAfter(end) && isAccruedCentsZero) {
            return VoucherStatus.EXPIRED.name();
        } else if (todayInExpiringWindow && isAccruedCentsZero) {
            return VoucherStatus.EXPIRING.name();
        } else if (todayIsBetweenInclusive && isAccruedCentsZero) {
            return VoucherStatus.ACTIVE.name();
        }
        return null;
    }
}
