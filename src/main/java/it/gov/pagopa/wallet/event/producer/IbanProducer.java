package it.gov.pagopa.wallet.event.producer;

import it.gov.pagopa.wallet.dto.IbanQueueDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class IbanProducer {
  @Value("${spring.cloud.stream.bindings.walletQueue-out-0.binder}")
  private String binderIban;
  @Autowired
  StreamBridge streamBridge;

  public void sendIban(IbanQueueDTO ibanQueueDTO){
    streamBridge.send("walletQueue-out-0", binderIban, ibanQueueDTO);
  }
}
