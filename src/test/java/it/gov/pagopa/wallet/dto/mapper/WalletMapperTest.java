package it.gov.pagopa.wallet.dto.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.*;
import it.gov.pagopa.wallet.enums.WalletStatus;
import it.gov.pagopa.wallet.model.Wallet;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
  private static final String ID_WALLET = "ID_WALLET";
  private static final LocalDate OPERATION_DATE = LocalDate.now();
  private static final LocalDateTime TEST_DATE = LocalDateTime.now();
  private static final String ORGANIZATION_NAME = "TEST_ORGANIZATION_NAME";

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
          .lastCounterUpdate(TEST_DATE)
          .initiativeRewardType(WalletConstants.INITIATIVE_REWARD_TYPE_REFUND)
          .organizationName(ORGANIZATION_NAME)
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
          new BigDecimal(500),
          WalletConstants.INITIATIVE_REWARD_TYPE_REFUND,
          ORGANIZATION_NAME);

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
          .lastCounterUpdate(TEST_DATE)
          .initiativeRewardType(WalletConstants.INITIATIVE_REWARD_TYPE_REFUND)
          .organizationName(ORGANIZATION_NAME)
          .build();

  private static final WalletDTO ISSUER_INITIATIVE_DTO =
      WalletDTO.builder()
          .amount(new BigDecimal(500))
          .accrued(BigDecimal.valueOf(0.00))
          .refunded(BigDecimal.valueOf(0.00))
          .lastCounterUpdate(TEST_DATE)
          .build();

  @Autowired WalletMapper walletMapper;

  @Test
  void map() {
    Wallet actual = walletMapper.map(EVALUATION_DTO);
    actual.setLastCounterUpdate(TEST_DATE);
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

  @Test
  void toInstrStatusOnInitiativeDTO(){
    InitiativesStatusDTO actual = walletMapper.toInstrStatusOnInitiativeDTO(INITIATIVE_DTO);

    assertEquals(INITIATIVE_DTO.getInitiativeId(), actual.getInitiativeId());
    assertEquals(INITIATIVE_DTO.getInitiativeName(), actual.getInitiativeName());
    assertEquals(WalletConstants.INSTRUMENT_STATUS_DEFAULT, actual.getStatus());
  }

  @Test
  void toInstrumentOnInitiativesDTO(){
    InstrumentDetailDTO instrDetail = new InstrumentDetailDTO("", "", new ArrayList<>());
    List<InitiativesStatusDTO> initiativeList = new ArrayList<>();
    InitiativesWithInstrumentDTO actual = walletMapper.toInstrumentOnInitiativesDTO(ID_WALLET,
            instrDetail, initiativeList);

    assertEquals(ID_WALLET, actual.getIdWallet());
    assertEquals(instrDetail.getMaskedPan(), actual.getMaskedPan());
    assertEquals(instrDetail.getBrand(), actual.getBrand());
    assertEquals(initiativeList, actual.getInitiativeList());
  }
}
