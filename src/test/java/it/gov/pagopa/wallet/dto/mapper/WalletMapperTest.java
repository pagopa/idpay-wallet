package it.gov.pagopa.wallet.dto.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.*;
import it.gov.pagopa.wallet.enums.Channel;
import it.gov.pagopa.wallet.enums.VoucherStatus;
import it.gov.pagopa.wallet.enums.WalletStatus;
import it.gov.pagopa.wallet.model.Wallet;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import it.gov.pagopa.wallet.utils.Utilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = WalletMapper.class)
@TestPropertySource(properties = "app.wallet.expiringDay=3")
class WalletMapperTest {

    @MockBean
    Utilities utilities;
    private static final String USER_ID = "test_user";
    private static final String FAMILY_ID = "test_family";
    private static final String INITIATIVE_ID = "test_initiative";
    private static final String ORGANIZATION_ID = "test_organization";
    private static final String ID_WALLET = "ID_WALLET";
    private static final LocalDate OPERATION_DATE = LocalDate.now();
    private static final LocalDateTime TEST_DATE = LocalDateTime.now();
    private static final String ORGANIZATION_NAME = "TEST_ORGANIZATION_NAME";
    private static final Long COUNTER_VERSION = 0L;
    private static final List<Long> COUNTER_HISTORY = new ArrayList<>();
    private static final String SERVICE_ID = "serviceid";
    private static final Wallet NEW_WALLET =
            Wallet.builder()
                    .id(USER_ID + "_" + INITIATIVE_ID)
                    .initiativeId(INITIATIVE_ID)
                    .initiativeName(INITIATIVE_ID)
                    .initiativeEndDate(OPERATION_DATE)
                    .voucherEndDate(OPERATION_DATE)
                    .voucherStartDate(OPERATION_DATE)
                    .organizationId(ORGANIZATION_ID)
                    .userId(USER_ID)
                    .familyId(FAMILY_ID)
                    .acceptanceDate(OPERATION_DATE.atStartOfDay())
                    .status(WalletStatus.NOT_REFUNDABLE.name())
                    .amountCents(50000L)
                    .accruedCents(0L)
                    .refundedCents(0L)
                    .lastCounterUpdate(TEST_DATE)
                    .initiativeRewardType(WalletConstants.INITIATIVE_REWARD_TYPE_REFUND)
                    .organizationName(ORGANIZATION_NAME)
                    .isLogoPresent(Boolean.TRUE)
                    .maxTrx(100L)
                    .counterVersion(COUNTER_VERSION)
                    .counterHistory(COUNTER_HISTORY)
                    //.serviceId(SERVICE_ID)
                    .build();

    private static final Wallet WALLET =
            Wallet.builder()
                    .id(USER_ID + "_" + INITIATIVE_ID)
                    .initiativeId(INITIATIVE_ID)
                    .initiativeName(INITIATIVE_ID)
                    .initiativeEndDate(OPERATION_DATE)
                    .voucherEndDate(OPERATION_DATE)
                    .voucherStartDate(OPERATION_DATE)
                    .organizationId(ORGANIZATION_ID)
                    .userId(USER_ID)
                    .familyId(FAMILY_ID)
                    .acceptanceDate(OPERATION_DATE.atStartOfDay())
                    .status(WalletStatus.NOT_REFUNDABLE.name())
                    .amountCents(49000L)
                    .accruedCents(1000L)
                    .refundedCents(100L)
                    .lastCounterUpdate(TEST_DATE)
                    .initiativeRewardType(WalletConstants.INITIATIVE_REWARD_TYPE_REFUND)
                    .organizationName(ORGANIZATION_NAME)
                    .isLogoPresent(Boolean.TRUE)
                    .nTrx(10L)
                    .maxTrx(100L)
                    .build();
    private static final Wallet WALLET_NO_LOGO =
            Wallet.builder()
                    .id(USER_ID + "_" + INITIATIVE_ID)
                    .initiativeId(INITIATIVE_ID)
                    .initiativeName(INITIATIVE_ID)
                    .initiativeEndDate(OPERATION_DATE)
                    .voucherEndDate(OPERATION_DATE)
                    .voucherStartDate(OPERATION_DATE)
                    .organizationId(ORGANIZATION_ID)
                    .userId(USER_ID)
                    .familyId(FAMILY_ID)
                    .acceptanceDate(OPERATION_DATE.atStartOfDay())
                    .status(WalletStatus.NOT_REFUNDABLE.name())
                    .amountCents(49000L)
                    .accruedCents(1000L)
                    .refundedCents(100L)
                    .lastCounterUpdate(TEST_DATE)
                    .initiativeRewardType(WalletConstants.INITIATIVE_REWARD_TYPE_REFUND)
                    .organizationName(ORGANIZATION_NAME)
                    .isLogoPresent(Boolean.FALSE)
                    .nTrx(10L)
                    .maxTrx(100L)
                    .build();

    private static final EvaluationDTO EVALUATION_DTO =
            new EvaluationDTO(
                    USER_ID,
                    FAMILY_ID,
                    INITIATIVE_ID,
                    INITIATIVE_ID,
                    OPERATION_DATE,
                    ORGANIZATION_ID,
                    WalletConstants.STATUS_ONBOARDING_OK,
                    OPERATION_DATE.atStartOfDay(),
                    OPERATION_DATE.atStartOfDay(),
                    List.of(),
                    50000L,
                    WalletConstants.INITIATIVE_REWARD_TYPE_REFUND,
                    ORGANIZATION_NAME,
                    Boolean.FALSE,
                    100L, SERVICE_ID, Channel.WEB);

    private static final WalletDTO INITIATIVE_DTO =
            WalletDTO.builder()
                    .familyId(FAMILY_ID)
                    .initiativeId(INITIATIVE_ID)
                    .initiativeName(INITIATIVE_ID)
                    .initiativeEndDate(OPERATION_DATE)
                    .voucherEndDate(OPERATION_DATE)
                    .voucherStartDate(OPERATION_DATE)
                    .status(WalletStatus.NOT_REFUNDABLE.name())
                    .voucherStatus(VoucherStatus.USED.name())
                    .amountCents(49000L)
                    .accruedCents(900L)
                    .refundedCents(100L)
                    .nInstr(0)
                    .lastCounterUpdate(TEST_DATE)
                    .initiativeRewardType(WalletConstants.INITIATIVE_REWARD_TYPE_REFUND)
                    .organizationName(ORGANIZATION_NAME)
                    .nTrx(10L)
                    .maxTrx(100L)
                    .build();

    private static final WalletDTO INITIATIVE_DTO_WITH_LOGO =
            WalletDTO.builder()
                    .familyId(FAMILY_ID)
                    .initiativeId(INITIATIVE_ID)
                    .initiativeName(INITIATIVE_ID)
                    .initiativeEndDate(OPERATION_DATE)
                    .voucherEndDate(OPERATION_DATE)
                    .voucherStartDate(OPERATION_DATE)
                    .status(WalletStatus.NOT_REFUNDABLE.name())
                    .voucherStatus(VoucherStatus.USED.name())
                    .amountCents(49000L)
                    .accruedCents(900L)
                    .refundedCents(100L)
                    .nInstr(0)
                    .lastCounterUpdate(TEST_DATE)
                    .initiativeRewardType(WalletConstants.INITIATIVE_REWARD_TYPE_REFUND)
                    .logoURL("https://test" + String.format(Utilities.LOGO_PATH_TEMPLATE,
                            ORGANIZATION_ID, INITIATIVE_ID, Utilities.LOGO_NAME))
                    .organizationName(ORGANIZATION_NAME)
                    .nTrx(10L)
                    .maxTrx(100L)
                    .build();

    private static final WalletDTO ISSUER_INITIATIVE_DTO =
            WalletDTO.builder()
                    .amountCents(49000L)
                    .accruedCents(900L)
                    .refundedCents(100L)
                    .lastCounterUpdate(TEST_DATE)
                    .build();

    @Autowired
    WalletMapper walletMapper;

    @Test
    void map() {
        Wallet actual = walletMapper.map(EVALUATION_DTO);
        actual.setLastCounterUpdate(TEST_DATE);
        actual.setIsLogoPresent(true);
        actual.setInitiativeEndDate(OPERATION_DATE);
        actual.setVoucherStartDate(OPERATION_DATE);
        actual.setVoucherEndDate(OPERATION_DATE);
        assertEquals(NEW_WALLET, actual);
    }

    @Test
    void toInitiativeDTO() {

        Mockito.when(utilities.createLogoUrl(WALLET.getOrganizationId(), WALLET.getInitiativeId()))
                .thenReturn("https://test" + String.format(Utilities.LOGO_PATH_TEMPLATE,
                        WALLET.getOrganizationId(), WALLET.getInitiativeId(), Utilities.LOGO_NAME));

        WalletDTO actual = walletMapper.toInitiativeDTO(WALLET);

        assertEquals(INITIATIVE_DTO_WITH_LOGO, actual);
    }

    @Test
    void toInitiativeDTO_noLogo() {

        WalletDTO actual = walletMapper.toInitiativeDTO(WALLET_NO_LOGO);

        assertEquals(INITIATIVE_DTO, actual);
    }

    @Test
    void to_issuer_InitiativeDTO() {
        WalletDTO actual = walletMapper.toIssuerInitiativeDTO(WALLET);

        assertEquals(ISSUER_INITIATIVE_DTO, actual);
    }

    @Test
    void toInstrStatusOnInitiativeDTO() {
        InitiativesStatusDTO actual = walletMapper.toInstrStatusOnInitiativeDTO(INITIATIVE_DTO);

        assertEquals(INITIATIVE_DTO.getInitiativeId(), actual.getInitiativeId());
        assertEquals(INITIATIVE_DTO.getInitiativeName(), actual.getInitiativeName());
        assertEquals(WalletConstants.INSTRUMENT_STATUS_DEFAULT, actual.getStatus());
    }

    @Test
    void toInstrumentOnInitiativesDTO() {
        InstrumentDetailDTO instrDetail = new InstrumentDetailDTO("", "", new ArrayList<>());
        List<InitiativesStatusDTO> initiativeList = new ArrayList<>();
        InitiativesWithInstrumentDTO actual = walletMapper.toInstrumentOnInitiativesDTO(ID_WALLET,
                instrDetail, initiativeList);

        assertEquals(ID_WALLET, actual.getIdWallet());
        assertEquals(instrDetail.getMaskedPan(), actual.getMaskedPan());
        assertEquals(instrDetail.getBrand(), actual.getBrand());
        assertEquals(initiativeList, actual.getInitiativeList());
    }

    @Test
    void setVoucherStatus_used_whenAccruedPositive() throws Exception {
        setExpiringDay(walletMapper, 3);
        LocalDate today = LocalDate.now();

        Wallet w = Wallet.builder()
                .voucherStartDate(today.minusDays(10))
                .voucherEndDate(today.plusDays(10))
                .accruedCents(1L) // > 0 -> USED
                .build();

        String status = invokeSetVoucherStatus(walletMapper, w);

        assertEquals(VoucherStatus.USED.name(), status);
    }

    @Test
    void setVoucherStatus_expired_whenAfterEnd_andAccruedZero() throws Exception {
        setExpiringDay(walletMapper, 3);
        LocalDate today = LocalDate.now();

        Wallet w = Wallet.builder()
                .voucherStartDate(today.minusDays(10))
                .voucherEndDate(today.minusDays(1)) // end < today
                .accruedCents(0L)
                .build();

        String status = invokeSetVoucherStatus(walletMapper, w);

        assertEquals(VoucherStatus.EXPIRED.name(), status);
    }

    @Test
    void setVoucherStatus_expiring_whenInExpiringWindow_andAccruedZero() throws Exception {
        setExpiringDay(walletMapper, 3);
        LocalDate today = LocalDate.now();
        // endM3 = end - 3; con end = today + 5 => endM3 = today + 2
        Wallet w = Wallet.builder()
                .voucherStartDate(today.minusDays(1)) // today > start
                .voucherEndDate(today.plusDays(5))    // today <= end - 3
                .accruedCents(0L)
                .build();

        String status = invokeSetVoucherStatus(walletMapper, w);

        assertEquals(VoucherStatus.EXPIRING.name(), status);
    }

    @Test
    void setVoucherStatus_active_whenBetweenInclusive_andAccruedZero() throws Exception {
        setExpiringDay(walletMapper, 3);
        LocalDate today = LocalDate.now();
        // today == start evita la finestra EXPIRING
        Wallet w = Wallet.builder()
                .voucherStartDate(today)
                .voucherEndDate(today.plusDays(5))
                .accruedCents(0L)
                .build();

        String status = invokeSetVoucherStatus(walletMapper, w);

        assertEquals(VoucherStatus.ACTIVE.name(), status);
    }

    private void setExpiringDay(Object target, int value) throws Exception {
        Field field = target.getClass().getDeclaredField("expiringDay");
        field.setAccessible(true);
        field.set(target, value);
    }

    private String invokeSetVoucherStatus(Object target, Wallet wallet) throws Exception {
        Method m = target.getClass().getDeclaredMethod("setVoucherStatus", Wallet.class);
        m.setAccessible(true);
        return (String) m.invoke(target, wallet);
    }
}
