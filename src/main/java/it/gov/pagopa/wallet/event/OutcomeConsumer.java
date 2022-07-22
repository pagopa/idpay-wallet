package it.gov.pagopa.wallet.event;

import it.gov.pagopa.wallet.dto.EvaluationDTO;
import it.gov.pagopa.wallet.service.WalletService;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OutcomeConsumer {

  @Bean
  public Consumer<EvaluationDTO> consumerTimeline(WalletService walletService) {
    return walletService::createWallet;
  }

}
