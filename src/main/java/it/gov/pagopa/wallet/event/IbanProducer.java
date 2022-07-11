package it.gov.pagopa.wallet.event;

import it.gov.pagopa.wallet.dto.IbanQueueDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class IbanProducer {
  @Value("${kafka.topic.iban}")
  private String topicIban;
  @Autowired
  StreamBridge streamBridge;

  public void sendIban(IbanQueueDTO ibanQueueDTO){
    streamBridge.send(topicIban, ibanQueueDTO);
  }
}
