package it.gov.pagopa.wallet.dto.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.EvaluationDTO;
import it.gov.pagopa.wallet.enums.WalletStatus;
import it.gov.pagopa.wallet.model.Wallet;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
  private static final LocalDateTime OPERATION_DATE = LocalDateTime.now();
  private static final Wallet WALLET =
      Wallet.builder()
          .initiativeId(INITIATIVE_ID)
          .initiativeName(INITIATIVE_ID)
          .endDate(OPERATION_DATE)
          .organizationId(INITIATIVE_ID)
          .userId(USER_ID)
          .serviceId(INITIATIVE_ID)
          .acceptanceDate(OPERATION_DATE)
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
          OPERATION_DATE,
          List.of(),
          new BigDecimal(500),
          INITIATIVE_ID);

  @Autowired WalletMapper walletMapper;

  @Test
  void map() {
    Wallet actual = walletMapper.map(EVALUATION_DTO);

    assertEquals(WALLET, actual);
  }
}
