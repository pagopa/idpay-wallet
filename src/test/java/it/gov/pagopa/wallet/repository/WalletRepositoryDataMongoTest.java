package it.gov.pagopa.wallet.repository;

import it.gov.pagopa.wallet.model.Wallet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataMongoTest(properties = {
        "spring.data.mongodb.database=test",
        "spring.data.mongodb.port=0",
        "de.flapdoodle.mongodb.embedded.version=4.2.24"
})
class WalletRepositoryDataMongoTest {

    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Rome");
    private static final String INITIATIVE_ID = "INITIATIVE_A";

    @Autowired
    private WalletRepository walletRepository;

    @AfterEach
    void cleanData() {
        walletRepository.deleteAll();
    }

    @Test
    void findVoucherExpiredIntoRange_shouldBindInstantRangeOnDstStartDate() {
        LocalDate target = LocalDate.of(2026, 3, 29);

        walletRepository.saveAll(List.of(
                wallet("w-target", "u-target", INITIATIVE_ID, target, 0L),
                wallet("w-prev", "u-prev", INITIATIVE_ID, target.minusDays(1), 0L),
                wallet("w-next", "u-next", INITIATIVE_ID, target.plusDays(1), 0L),
                wallet("w-accrued", "u-accrued", INITIATIVE_ID, target, 10L),
                wallet("w-other-init", "u-other-init", "INITIATIVE_B", target, 0L)
        ));

        Instant startUtc = target.atStartOfDay(ZONE_ID).toInstant();
        Instant endUtc = target.plusDays(1).atStartOfDay(ZONE_ID).toInstant();
        Pageable pageable = PageRequest.of(0, 10);

        Page<Wallet> result = walletRepository.findVoucherExpiredIntoRange(INITIATIVE_ID, startUtc, endUtc, pageable);

        assertEquals(23L, Duration.between(startUtc, endUtc).toHours());
        assertEquals(1L, result.getTotalElements());
        assertEquals("u-target", result.getContent().getFirst().getUserId());
    }

    @Test
    void findVoucherExpiredIntoRange_shouldBindInstantRangeOnDstEndDate() {
        LocalDate target = LocalDate.of(2026, 10, 25);

        walletRepository.saveAll(List.of(
                wallet("w-target-dst-end", "u-target-dst-end", INITIATIVE_ID, target, 0L),
                wallet("w-prev-dst-end", "u-prev-dst-end", INITIATIVE_ID, target.minusDays(1), 0L),
                wallet("w-next-dst-end", "u-next-dst-end", INITIATIVE_ID, target.plusDays(1), 0L)
        ));

        Instant startUtc = target.atStartOfDay(ZONE_ID).toInstant();
        Instant endUtc = target.plusDays(1).atStartOfDay(ZONE_ID).toInstant();
        Pageable pageable = PageRequest.of(0, 10);

        Page<Wallet> result = walletRepository.findVoucherExpiredIntoRange(INITIATIVE_ID, startUtc, endUtc, pageable);

        assertEquals(25L, Duration.between(startUtc, endUtc).toHours());
        assertEquals(1L, result.getTotalElements());
        assertEquals("u-target-dst-end", result.getContent().getFirst().getUserId());
    }

    private Wallet wallet(String id, String userId, String initiativeId, LocalDate voucherEndDate, Long accruedCents) {
        return Wallet.builder()
                .id(id)
                .userId(userId)
                .initiativeId(initiativeId)
                .voucherEndDate(voucherEndDate)
                .accruedCents(accruedCents)
                .build();
    }
}

