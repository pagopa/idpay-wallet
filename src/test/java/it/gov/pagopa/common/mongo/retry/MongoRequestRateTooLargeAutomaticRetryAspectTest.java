package it.gov.pagopa.common.mongo.retry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

class MongoRequestRateTooLargeAutomaticRetryAspectTest {
    private final boolean enabledApi;
    private final long maxRetryApi;
    private final long maxMillisElapsedApi;
    private final boolean enabledBatch;
    private final long maxRetryBatch;
    private final long maxMillisElapsedBatch;

    MongoRequestRateTooLargeAutomaticRetryAspectTest(
            @Value("${mongo.request-rate-too-large.api.enabled}") boolean enabledApi,
            @Value("${mongo.request-rate-too-large.api.max-retry:3}") long maxRetryApi,
            @Value("${mongo.request-rate-too-large.api.max-millis-elapsed:0}") long maxMillisElapsedApi,
            @Value("${mongo.request-rate-too-large.batch.enabled}") boolean enabledBatch,
            @Value("${mongo.request-rate-too-large.batch.max-retry}") long maxRetryBatch,
            @Value("${mongo.request-rate-too-large.batch.max-millis-elapsed}") long maxMillisElapsedBatch) {
        this.enabledApi = enabledApi;
        this.maxRetryApi = maxRetryApi;
        this.maxMillisElapsedApi = maxMillisElapsedApi;
        this.enabledBatch = enabledBatch;
        this.maxRetryBatch = maxRetryBatch;
        this.maxMillisElapsedBatch = maxMillisElapsedBatch;
    }

    @Test
    void decorateRepositoryMethodsAPI_ControllerContextAndEnabledFalse() {


    }
}