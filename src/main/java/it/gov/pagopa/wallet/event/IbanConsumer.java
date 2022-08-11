package it.gov.pagopa.wallet.event;

import it.gov.pagopa.wallet.dto.IbanQueueWalletDTO;
import it.gov.pagopa.wallet.service.WalletService;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IbanConsumer {

  @Bean
  public Consumer<IbanQueueWalletDTO> consumerIban(WalletService walletService){
    return walletService::deleteOperation;
  }

}
