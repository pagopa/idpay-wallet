package it.gov.pagopa.wallet.event.producer;

import it.gov.pagopa.wallet.dto.NotificationQueueDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class NotificationProducer {
  @Value("${spring.cloud.stream.bindings.walletQueue-out-2.binder}")
  private String binder;
  @Autowired
  StreamBridge streamBridge;

  public void sendNotification(NotificationQueueDTO notificationQueueDTO){
    streamBridge.send("walletQueue-out-2", binder, notificationQueueDTO);
  }
}
