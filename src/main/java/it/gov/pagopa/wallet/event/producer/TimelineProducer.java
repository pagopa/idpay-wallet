package it.gov.pagopa.wallet.event.producer;

import it.gov.pagopa.wallet.dto.QueueOperationDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class TimelineProducer {
  private final String binderTimeline;
  private final StreamBridge streamBridge;

  public TimelineProducer(@Value("${spring.cloud.stream.bindings.walletQueue-out-1.binder}") String binderTimeline,
                          StreamBridge streamBridge) {
    this.binderTimeline = binderTimeline;
    this.streamBridge = streamBridge;
  }

  public void sendEvent(QueueOperationDTO queueOperationDTO){
    streamBridge.send("walletQueue-out-1",binderTimeline, queueOperationDTO);
  }
}
