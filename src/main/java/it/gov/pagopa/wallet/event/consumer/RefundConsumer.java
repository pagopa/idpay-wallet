package it.gov.pagopa.wallet.event.consumer;

import it.gov.pagopa.wallet.dto.RefundDTO;
import it.gov.pagopa.wallet.service.WalletService;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RefundConsumer {

  @Bean
  public Consumer<RefundDTO> consumerRefund(WalletService walletService){
    return walletService::processRefund;
  }

}
