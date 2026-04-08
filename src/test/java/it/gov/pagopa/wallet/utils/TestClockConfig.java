package it.gov.pagopa.wallet.utils;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

@TestConfiguration
public class TestClockConfig {

  public static final Instant FIXED_INSTANT =
      Instant.parse("2024-01-01T10:00:00Z");

  @Bean
  public Clock clock() {
    return Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
  }
}
