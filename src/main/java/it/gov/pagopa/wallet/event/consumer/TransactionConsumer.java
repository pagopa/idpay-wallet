package it.gov.pagopa.wallet.event.consumer;

import it.gov.pagopa.wallet.dto.RewardTransactionDTO;
import it.gov.pagopa.wallet.service.WalletService;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransactionConsumer {

  @Bean
  public Consumer<RewardTransactionDTO> trxConsumer(WalletService walletService){
    return walletService::processTransaction;
  }
}
