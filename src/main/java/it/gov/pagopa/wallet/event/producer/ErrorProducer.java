package it.gov.pagopa.wallet.event.producer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
public class ErrorProducer {
  private final String binderError;

  private final StreamBridge streamBridge;

  public ErrorProducer(@Value("${spring.cloud.stream.bindings.walletQueue-out-3.binder}") String binderError,
                       StreamBridge streamBridge) {
    this.binderError = binderError;
    this.streamBridge = streamBridge;
  }

  public void sendEvent(Message<?> errorQueueDTO){
    streamBridge.send("walletQueue-out-3",binderError, errorQueueDTO);
  }
}
