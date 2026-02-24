package it.gov.pagopa.common.stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.actuate.endpoint.HealthDescriptor;
import org.springframework.boot.health.autoconfigure.actuate.endpoint.HealthEndpointAutoConfiguration;
import org.springframework.boot.health.contributor.Status;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {StreamsHealthIndicator.class, HealthEndpointAutoConfiguration.class})
class StreamsHealthIndicatorTest {

    @Autowired
    private HealthEndpoint healthEndpoint;
    @Autowired
    private StreamsHealthIndicator indicator;

    @Test
    void test() {
        performHealthCheck(Status.UP);

        indicator.afterSendCompletion(MessageBuilder.withPayload("MESSAGE").build(), Mockito.mock(MessageChannel.class), false, new RuntimeException());
        performHealthCheck(Status.UP);

        indicator.afterSendCompletion(MessageBuilder.withPayload("MESSAGE").build(), Mockito.mock(MessageChannel.class), false, new IllegalStateException(""));
        performHealthCheck(Status.UP);

        indicator.afterSendCompletion(MessageBuilder.withPayload("MESSAGE").build(), Mockito.mock(MessageChannel.class), false, new IllegalStateException("The [bean 'dummy_channel'] doesn't have subscribers to accept messages"));
        performHealthCheck(Status.DOWN);
    }

    private HealthDescriptor performHealthCheck(Status expectedStatus) {
        HealthDescriptor health = healthEndpoint.health();
        Assertions.assertEquals(expectedStatus, health.getStatus());
        return health;
    }
}
