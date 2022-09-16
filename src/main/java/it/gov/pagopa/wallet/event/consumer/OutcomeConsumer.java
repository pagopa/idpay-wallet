package it.gov.pagopa.wallet.event.consumer;

import it.gov.pagopa.wallet.dto.EvaluationDTO;
import it.gov.pagopa.wallet.service.WalletService;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OutcomeConsumer {

  @Bean
  public Consumer<EvaluationDTO> consumerOutcome(WalletService walletService) {
    return walletService::createWallet;
  }

}
