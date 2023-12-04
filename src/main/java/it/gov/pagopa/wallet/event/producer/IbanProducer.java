package it.gov.pagopa.wallet.event.producer;

import it.gov.pagopa.wallet.dto.IbanQueueDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class IbanProducer {
  private final String binderIban;
  private final StreamBridge streamBridge;

  public IbanProducer(@Value("${spring.cloud.stream.bindings.walletQueue-out-0.binder}") String binderIban,
                      StreamBridge streamBridge) {
    this.binderIban = binderIban;
    this.streamBridge = streamBridge;
  }

  public void sendIban(IbanQueueDTO ibanQueueDTO){
    streamBridge.send("walletQueue-out-0", binderIban, buildMessage(ibanQueueDTO));
  }

  public static Message<IbanQueueDTO> buildMessage(IbanQueueDTO ibanQueueDTO){
    return MessageBuilder.withPayload(ibanQueueDTO)
            .setHeader(KafkaHeaders.KEY,"%s_%s".formatted(ibanQueueDTO.getUserId(), ibanQueueDTO.getInitiativeId()))
            .build();
  }
}
