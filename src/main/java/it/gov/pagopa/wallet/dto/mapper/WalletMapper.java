package it.gov.pagopa.wallet.dto.mapper;

import it.gov.pagopa.wallet.dto.*;
import it.gov.pagopa.wallet.enums.WalletStatus;
import it.gov.pagopa.wallet.model.Wallet;
import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class WalletMapper {

  public Wallet map(EvaluationDTO evaluationDTO) {
    return Wallet.builder()
        .initiativeId(evaluationDTO.getInitiativeId())
        .initiativeName(evaluationDTO.getInitiativeName())
        .endDate(evaluationDTO.getInitiativeEndDate())
        .organizationId(evaluationDTO.getOrganizationId())
        .userId(evaluationDTO.getUserId())
        .acceptanceDate(evaluationDTO.getAdmissibilityCheckDate())
        .status(WalletStatus.NOT_REFUNDABLE.name())
        .amount(evaluationDTO.getBeneficiaryBudget())
        .accrued(BigDecimal.valueOf(0.00))
        .refunded(BigDecimal.valueOf(0.00))
        .build();
  }

  public WalletDTO toInitiativeDTO(Wallet wallet){
    return WalletDTO.builder()
        .initiativeId(wallet.getInitiativeId())
        .initiativeName(wallet.getInitiativeName())
        .endDate(wallet.getEndDate())
        .status(wallet.getStatus())
        .amount(wallet.getAmount())
        .accrued(wallet.getAccrued())
        .refunded(wallet.getRefunded())
        .nInstr(wallet.getNInstr())
        .iban(wallet.getIban())
        .lastCounterUpdate(wallet.getLastCounterUpdate())
        .build();
  }

  public WalletDTO toIssuerInitiativeDTO(Wallet wallet){
    return WalletDTO.builder()
        .amount(wallet.getAmount())
        .accrued(wallet.getAccrued())
        .refunded(wallet.getRefunded())
        .lastCounterUpdate(wallet.getLastCounterUpdate())
        .build();
  }

  public InstrumentStatusOnInitiativeDTO toInstrStatusOnInitiativeDTO(WalletDTO wallet){
    return InstrumentStatusOnInitiativeDTO.builder()
            .initiativeId(wallet.getInitiativeId())
            .initiativeName(wallet.getInitiativeName())
            .build();
  }

  public InstrumentOnInitiativesDTO instrumentOnInitiativesDTO(String idWallet,
                                                               InstrumentDetailDTO instrumentDetailDTO,
                                                               List<InstrumentStatusOnInitiativeDTO> instrumentStatusOnInitiativeDTO){
    return InstrumentOnInitiativesDTO.builder()
            .idWallet(idWallet)
            .idInstrument(instrumentDetailDTO.getIdInstrument())
            .maskedPan(instrumentDetailDTO.getMaskedPan())
            .brandLogo(instrumentDetailDTO.getBrandLogo())
            .brand(instrumentDetailDTO.getBrand())
            .initiativeList(instrumentStatusOnInitiativeDTO).build();
  }
}
