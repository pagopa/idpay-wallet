package it.gov.pagopa.wallet.dto.mapper;

import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.EvaluationDTO;
import it.gov.pagopa.wallet.model.Wallet;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class WalletMapper {

  public Wallet map(EvaluationDTO evaluationDTO) {
    return Wallet.builder()
        .initiativeId(evaluationDTO.getInitiativeId())
        .userId(evaluationDTO.getUserId())
        .acceptanceDate(evaluationDTO.getAdmissibilityCheckDate())
        .status(WalletConstants.STATUS_NOT_REFUNDABLE)
        .accrued(BigDecimal.valueOf(0.00))
        .refunded(BigDecimal.valueOf(0.00))
        .build();
  }
}
