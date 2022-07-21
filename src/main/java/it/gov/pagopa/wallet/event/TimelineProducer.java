package it.gov.pagopa.wallet.event;

import it.gov.pagopa.wallet.dto.QueueOperationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class TimelineProducer {
  @Value("${spring.cloud.stream.bindings.walletQueue-out-1.binder}")
  private String binderInstrument;
  @Autowired
  StreamBridge streamBridge;

  public void sendInstrument(QueueOperationDTO queueOperationDTO){
    streamBridge.send("walletQueue-out-1",binderInstrument, queueOperationDTO);
  }
}
