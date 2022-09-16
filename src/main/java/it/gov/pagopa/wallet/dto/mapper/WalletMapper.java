package it.gov.pagopa.wallet.dto.mapper;

import it.gov.pagopa.wallet.dto.EvaluationDTO;
import it.gov.pagopa.wallet.dto.InitiativeDTO;
import it.gov.pagopa.wallet.enums.WalletStatus;
import it.gov.pagopa.wallet.model.Wallet;
import java.math.BigDecimal;
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
        .serviceId(evaluationDTO.getServiceId())
        .acceptanceDate(evaluationDTO.getAdmissibilityCheckDate())
        .status(WalletStatus.NOT_REFUNDABLE.name())
        .amount(evaluationDTO.getBeneficiaryBudget())
        .accrued(BigDecimal.valueOf(0.00))
        .refunded(BigDecimal.valueOf(0.00))
        .build();
  }

  public InitiativeDTO toInitiativeDTO(Wallet wallet){
    return InitiativeDTO.builder()
        .initiativeId(wallet.getInitiativeId())
        .initiativeName(wallet.getInitiativeName())
        .endDate(wallet.getEndDate())
        .status(wallet.getStatus())
        .amount(wallet.getAmount())
        .accrued(wallet.getAccrued())
        .refunded(wallet.getRefunded())
        .nInstr(String.valueOf(wallet.getNInstr()))
        .iban(wallet.getIban())
        .build();
  }
}
