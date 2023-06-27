package it.gov.pagopa.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "it.gov.pagopa")
public class WalletApplication {

  public static void main(String[] args) {
    SpringApplication.run(WalletApplication.class, args);
  }

}
