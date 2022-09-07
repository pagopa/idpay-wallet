package it.gov.pagopa.wallet.event.producer;

import it.gov.pagopa.wallet.dto.QueueOperationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class TimelineProducer {

  @Value("${spring.cloud.stream.bindings.walletQueue-out-1.binder}")
  private String binderTimeline;
  
  @Autowired
  StreamBridge streamBridge;

  public void sendEvent(QueueOperationDTO queueOperationDTO){
    streamBridge.send("walletQueue-out-1",binderTimeline, queueOperationDTO);
  }
}
