package it.gov.pagopa.wallet.dto.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.EvaluationDTO;
import it.gov.pagopa.wallet.dto.WalletDTO;
import it.gov.pagopa.wallet.enums.WalletStatus;
import it.gov.pagopa.wallet.model.Wallet;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = WalletMapper.class)
class WalletMapperTest {
  private static final String USER_ID = "test_user";
  private static final String INITIATIVE_ID = "test_initiative";
  private static final LocalDate OPERATION_DATE = LocalDate.now();
  private static final Wallet WALLET =
      Wallet.builder()
          .initiativeId(INITIATIVE_ID)
          .initiativeName(INITIATIVE_ID)
          .endDate(OPERATION_DATE)
          .organizationId(INITIATIVE_ID)
          .userId(USER_ID)
          .acceptanceDate(OPERATION_DATE.atStartOfDay())
          .status(WalletStatus.NOT_REFUNDABLE.name())
          .amount(new BigDecimal(500))
          .accrued(BigDecimal.valueOf(0.00))
          .refunded(BigDecimal.valueOf(0.00))
          .build();
  private static final EvaluationDTO EVALUATION_DTO =
      new EvaluationDTO(
          USER_ID,
          INITIATIVE_ID,
          INITIATIVE_ID,
          OPERATION_DATE,
          INITIATIVE_ID,
          WalletConstants.STATUS_ONBOARDING_OK,
          OPERATION_DATE.atStartOfDay(),
          OPERATION_DATE.atStartOfDay(),
          List.of(),
          new BigDecimal(500));

  private static final WalletDTO INITIATIVE_DTO =
      WalletDTO.builder()
          .initiativeId(INITIATIVE_ID)
          .initiativeName(INITIATIVE_ID)
          .endDate(OPERATION_DATE)
          .status(WalletStatus.NOT_REFUNDABLE.name())
          .amount(new BigDecimal(500))
          .accrued(BigDecimal.valueOf(0.00))
          .refunded(BigDecimal.valueOf(0.00))
          .nInstr(0)
          .build();

  private static final WalletDTO ISSUER_INITIATIVE_DTO =
      WalletDTO.builder()
          .amount(new BigDecimal(500))
          .accrued(BigDecimal.valueOf(0.00))
          .refunded(BigDecimal.valueOf(0.00))
          .build();

  @Autowired WalletMapper walletMapper;

  @Test
  void map() {
    Wallet actual = walletMapper.map(EVALUATION_DTO);

    assertEquals(WALLET, actual);
  }

  @Test
  void toInitiativeDTO(){
    WalletDTO actual = walletMapper.toInitiativeDTO(WALLET);

    assertEquals(INITIATIVE_DTO, actual);
  }

  @Test
  void to_issuer_InitiativeDTO(){
    WalletDTO actual = walletMapper.toIssuerInitiativeDTO(WALLET);

    assertEquals(ISSUER_INITIATIVE_DTO, actual);
  }
}
