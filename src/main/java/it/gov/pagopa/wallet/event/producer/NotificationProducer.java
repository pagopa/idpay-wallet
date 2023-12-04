package it.gov.pagopa.wallet.event.producer;

import it.gov.pagopa.wallet.dto.NotificationQueueDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class NotificationProducer {
  private final String binder;
  private final StreamBridge streamBridge;

  public NotificationProducer(@Value("${spring.cloud.stream.bindings.walletQueue-out-2.binder}") String binder,
                              StreamBridge streamBridge) {
    this.binder = binder;
    this.streamBridge = streamBridge;
  }

  public void sendNotification(NotificationQueueDTO notificationQueueDTO){
    streamBridge.send("walletQueue-out-2", binder, notificationQueueDTO);
  }
}
