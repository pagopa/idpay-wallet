package it.gov.pagopa.common.mongo.retry;


import it.gov.pagopa.common.mongo.DummySpringRepository;
import it.gov.pagopa.common.mongo.config.MongoConfig;
import it.gov.pagopa.common.mongo.retry.exception.MongoRequestRateTooLargeRetryExpiredException;
import it.gov.pagopa.common.mongo.singleinstance.AutoConfigureSingleInstanceMongodb;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.common.web.exception.ErrorManager;
import it.gov.pagopa.common.web.exception.MongoExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@TestPropertySource(
    properties = {
        "de.flapdoodle.mongodb.embedded.version=4.2.24",
        "management.health.mongo.enabled=false",
        "spring.mongodb.database=idpay",
        "spring.mongodb.config.connectionPool.maxSize: 100",
        "spring.mongodb.config.connectionPool.minSize: 0",
        "spring.mongodb.config.connectionPool.maxWaitTimeMS: 120000",
        "spring.mongodb.config.connectionPool.maxConnectionLifeTimeMS: 0",
        "spring.mongodb.config.connectionPool.maxConnectionIdleTimeMS: 120000",
        "spring.mongodb.config.connectionPool.maxConnecting: 2",
    })
@ContextConfiguration(classes = {
    MongoRequestRateTooLargeAutomaticRetryAspect.class,
    MongoExceptionHandler.class,
    MongoConfig.class,

    MongoRequestRateTooLargeRetryIntegrationTest.TestController.class,
    MongoRequestRateTooLargeRetryIntegrationTest.TestRepository.class,
    MongoRequestRateTooLargeRetryIntegrationTest.TestConfig.class
})
@WebMvcTest(excludeAutoConfiguration = { UserDetailsServiceAutoConfiguration.class , SecurityAutoConfiguration.class,})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureSingleInstanceMongodb
class MongoRequestRateTooLargeRetryIntegrationTest {

  @TestConfiguration
  static class TestConfig {
    @Bean
    public ErrorManager errorManager() {
      return new ErrorManager(new ErrorDTO());
    }
  }

  @Value("${mongo.request-rate-too-large.batch.max-retry:3}")
  private int maxRetry;
  @Value("${mongo.request-rate-too-large.batch.max-millis-elapsed:0}")
  private int maxMillisElapsed;

  private static final int API_RETRYABLE_MAX_RETRY = 5;

  @MockitoSpyBean
  private TestRepository testRepositorySpy;
  @Autowired
  private DummySpringRepository dummySpringRepository;

  @MockitoSpyBean
  private MongoRequestRateTooLargeAutomaticRetryAspect automaticRetryAspectSpy;

  @MockitoSpyBean
  private ErrorManager errorManager;

  private static int[] counter;

  @BeforeEach
  void init() {
    counter = new int[]{0};
  }

  @RestController
  @Slf4j
  static class TestController {

    @Autowired
    private TestRepository repository;

    @GetMapping("/test")
    String testEndpoint() {
      return buildNestedRepositoryMethodInvoke(repository);
    }

    @MongoRequestRateTooLargeApiRetryable(maxRetry = API_RETRYABLE_MAX_RETRY)
    @GetMapping("/test-api-retryable")
    String testEndpointRetryable() {
      return buildNestedRepositoryMethodInvoke(repository);
    }

    static String buildNestedRepositoryMethodInvoke(TestRepository repository) {
      return repository.test();
    }
  }

  @Service
  static class TestRepository {
    public String test() {
      counter[0]++;
      throw MongoRequestRateTooLargeRetryerTest.buildRequestRateTooLargeMongodbException_whenReading();

    }
  }

  @Autowired
  private MockMvc mockMvc;

  @Test
  void testController_Method() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/test")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isTooManyRequests())
        .andExpect(MockMvcResultMatchers.content().json("{\"message\":\"Too Many Requests\"}", false));

    Assertions.assertEquals(1, counter[0]);
  }

  @Test
  void testControllerRetryable_Method() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/test-api-retryable")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isTooManyRequests())
        .andExpect(MockMvcResultMatchers.content().json("{\"message\":\"Too Many Requests\"}", false));

    Assertions.assertEquals(counter[0], API_RETRYABLE_MAX_RETRY + 1);
  }

  @Test
  void testNoController_Method() {
    try {
      TestController.buildNestedRepositoryMethodInvoke(testRepositorySpy);
      Assertions.fail("Expected exception");
    } catch (MongoRequestRateTooLargeRetryExpiredException e) {
      Assertions.assertEquals(maxRetry + 1, e.getCounter());
      Assertions.assertEquals(maxRetry, e.getMaxRetry());
      Assertions.assertEquals(maxMillisElapsed, e.getMaxMillisElapsed());
      Assertions.assertTrue(e.getMillisElapsed() > 0);
    }

    Assertions.assertEquals(counter[0], maxRetry + 1);
  }

  @Test
  void testSpringRepositoryInterceptor() throws Throwable {
    // When
    dummySpringRepository.findByIdOrderById("ID");

    // Then
    Mockito.verify(automaticRetryAspectSpy).decorateRepositoryMethods(Mockito.argThat(i -> i.getArgs()[0].equals("ID")));
  }
}
