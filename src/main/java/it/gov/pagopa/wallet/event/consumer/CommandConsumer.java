package it.gov.pagopa.wallet.event.consumer;

import it.gov.pagopa.wallet.dto.QueueCommandOperationDTO;
import it.gov.pagopa.wallet.service.WalletService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class CommandConsumer {

    @Bean
    public Consumer<QueueCommandOperationDTO> consumerCommand(WalletService walletService) {
        return walletService::processCommand;
    }

}
