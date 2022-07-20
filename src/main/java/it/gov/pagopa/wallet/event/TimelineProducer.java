package it.gov.pagopa.wallet.event;

import it.gov.pagopa.wallet.dto.QueueOperationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class TimelineProducer {

  @Value("${spring.cloud.stream.bindings.walletQueue-out-1.binder}")
  private String outcomeBinder;

  @Autowired
  StreamBridge streamBridge;

  public void sendTimelineEvent(QueueOperationDTO queueOperationDTO){
    streamBridge.send("walletQueue-out-1", outcomeBinder, queueOperationDTO);
  }

}
