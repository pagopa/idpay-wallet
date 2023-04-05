package it.gov.pagopa.wallet.connector;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.mongodb.assertions.Assertions;
import it.gov.pagopa.wallet.config.WalletConfig;
import it.gov.pagopa.wallet.dto.UnsubscribeCallDTO;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.TestPropertySourceUtils;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(
    initializers = OnboardingRestClientTest.WireMockInitializer.class,
    classes = {
      OnboardingRestConnectorImpl.class,
      WalletConfig.class,
      FeignAutoConfiguration.class,
      HttpMessageConvertersAutoConfiguration.class
    })
@TestPropertySource(
    locations = "classpath:application.yml",
    properties = {"spring.application.name=idpay-onboarding-integration-rest"})
class OnboardingRestClientTest {

  private static final String USER_ID = "USER_ID";
  private static final String INITIATIVE_ID = "INITIATIVE_ID";

  @Autowired private OnboardingRestClient restClient;

  @Autowired private OnboardingRestConnector restConnector;

  @Test
  void disable_onboarding_test() {

    final UnsubscribeCallDTO instrument =
        new UnsubscribeCallDTO(INITIATIVE_ID, USER_ID, LocalDateTime.now().toString());

    try{
      restConnector.disableOnboarding(instrument);
    } catch (Exception e){
      Assertions.fail();
    }

  }

  @Test
  void rollback_test() {
    try{
      restConnector.rollback(INITIATIVE_ID, USER_ID);
    } catch (Exception e){
      Assertions.fail();
    }
  }
  @Test
  void suspend_test() {
    try{
      restConnector.suspendOnboarding(INITIATIVE_ID, USER_ID);
    } catch (Exception e){
      Assertions.fail();
    }
  }
  @Test
  void readmit_test() {
    try{
      restConnector.readmitOnboarding(INITIATIVE_ID, USER_ID);
    } catch (Exception e){
      Assertions.fail();
    }
  }

  public static class WireMockInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      WireMockServer wireMockServer = new WireMockServer(new WireMockConfiguration().dynamicPort());
      wireMockServer.start();

      applicationContext.getBeanFactory().registerSingleton("wireMockServer", wireMockServer);

      applicationContext.addApplicationListener(
          applicationEvent -> {
            if (applicationEvent instanceof ContextClosedEvent) {
              wireMockServer.stop();
            }
          });

      TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
          applicationContext,
          String.format(
              "rest-client.onboarding.baseUrl=http://%s:%d",
              wireMockServer.getOptions().bindAddress(), wireMockServer.port()));
    }
  }
}
