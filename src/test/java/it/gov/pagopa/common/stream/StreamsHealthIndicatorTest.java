package it.gov.pagopa.common.stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cloud.stream.messaging.DirectWithAttributesChannel;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Collections;


class StreamsHealthIndicatorTest {

    @Test
    void testHealthIndicator() {
        ApplicationContext mockContext = Mockito.mock(ApplicationContext.class);
        Mockito.when(mockContext.getBeansOfType(DirectWithAttributesChannel.class))
                .thenReturn(Collections.emptyMap());

        StreamsHealthIndicator indicator = new StreamsHealthIndicator(mockContext);

        // initially up because there are no disconnected subscribers
        Assertions.assertEquals("UP", indicator.health().getStatus().getCode());

        // send a message with a runtime exception (should not affect health)
        indicator.afterSendCompletion(MessageBuilder.withPayload("MESSAGE").build(), Mockito.mock(MessageChannel.class), false, new RuntimeException());
        Assertions.assertEquals("UP", indicator.health().getStatus().getCode());

        // send a message with IllegalStateException not related to subscriber
        indicator.afterSendCompletion(MessageBuilder.withPayload("MESSAGE").build(), Mockito.mock(MessageChannel.class), false, new IllegalStateException(""));
        Assertions.assertEquals("UP", indicator.health().getStatus().getCode());

        // send a message with subscriber disconnected exception
        indicator.afterSendCompletion(
                MessageBuilder.withPayload("MESSAGE").build(),
                Mockito.mock(MessageChannel.class),
                false,
                new IllegalStateException("The [bean 'dummy_channel'] doesn't have subscribers to accept messages")
        );
        Assertions.assertEquals("DOWN", indicator.health().getStatus().getCode());
    }
}
