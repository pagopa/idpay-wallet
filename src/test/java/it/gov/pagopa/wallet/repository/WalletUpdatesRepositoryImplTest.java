package it.gov.pagopa.wallet.repository;

import it.gov.pagopa.wallet.model.Wallet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    WalletUpdatesRepositoryImpl.class,
    WalletUpdatesRepositoryImplTest.TestConfig.class
})
class WalletUpdatesRepositoryImplTest {

  @Autowired
  private WalletUpdatesRepository walletUpdatesRepository;

  @MockitoBean
  private MongoTemplate mongoTemplate;

  private static final String INITIATIVE_ID = "INITIATIVE_ID";
  private static final String USER_ID = "USER_ID";
  private static final String STATUS = "ACTIVE";
  private static final String IBAN = "IT60X0542811101000000123456";

  private static final Instant NOW = Instant.parse("2024-01-01T10:00:00Z");

  @Configuration
  static class TestConfig {
    @Bean
    Clock clock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }
  }

  @Test
  void deleteIban() {
    walletUpdatesRepository.deleteIban(INITIATIVE_ID, USER_ID, STATUS);

    verify(mongoTemplate, times(1)).updateFirst(
        any(Query.class),
        any(Update.class),
        eq(Wallet.class)
    );
  }

  @Test
  void enrollIban() {
    walletUpdatesRepository.enrollIban(INITIATIVE_ID, USER_ID, IBAN, STATUS);

    verify(mongoTemplate, times(1)).updateFirst(
        any(Query.class),
        any(Update.class),
        eq(Wallet.class)
    );
  }
  @Test
  void suspendWallet() {
    walletUpdatesRepository.suspendWallet(INITIATIVE_ID, USER_ID, STATUS, NOW);

    verify(mongoTemplate, times(1)).updateFirst(
        any(Query.class),
        any(Update.class),
        eq(Wallet.class)
    );
  }

  @Test
  void readmitWallet() {
    walletUpdatesRepository.readmitWallet(INITIATIVE_ID, USER_ID, STATUS, NOW);

    verify(mongoTemplate, times(1)).updateFirst(
        any(Query.class),
        any(Update.class),
        eq(Wallet.class)
    );
  }

  @Test
  void processRefund() {
    Map<String, Wallet.RefundHistory> history = Map.of();

    walletUpdatesRepository.processRefund(INITIATIVE_ID, USER_ID, 100L, history);

    verify(mongoTemplate, times(1)).updateFirst(
        any(Query.class),
        any(Update.class),
        eq(Wallet.class)
    );
  }

  @Test
  void updateInstrumentNumber() {
    walletUpdatesRepository.updateInstrumentNumber(INITIATIVE_ID, USER_ID, 2, STATUS);

    verify(mongoTemplate, times(1)).updateFirst(
        any(Query.class),
        any(Update.class),
        eq(Wallet.class)
    );
  }

  @Test
  void decreaseInstrumentNumber() {
    walletUpdatesRepository.decreaseInstrumentNumber(INITIATIVE_ID, USER_ID, STATUS);

    verify(mongoTemplate, times(1)).updateFirst(
        any(Query.class),
        any(Update.class),
        eq(Wallet.class)
    );
  }

  @Test
  void deletePaged() {
    int pageSize = 10;
    Wallet wallet = Wallet.builder().build();

    when(mongoTemplate.findAllAndRemove(any(Query.class), eq(Wallet.class)))
        .thenReturn(List.of(wallet));

    List<Wallet> result = walletUpdatesRepository.deletePaged(INITIATIVE_ID, pageSize);

    Assertions.assertEquals(1, result.size());

    verify(mongoTemplate, times(1)).findAllAndRemove(
        any(Query.class),
        eq(Wallet.class)
    );
  }
}
