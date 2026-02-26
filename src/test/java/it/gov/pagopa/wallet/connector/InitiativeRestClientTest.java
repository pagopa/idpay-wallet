package it.gov.pagopa.wallet.connector;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;


class InitiativeRestClientTest {

  private static final String INITIATIVE_ID = "INITIATIVE_ID";

  private InitiativeRestClient restClient;

  private InitiativeRestConnectorImpl restConnector;


  @BeforeEach
  void setUp() {
    restClient = Mockito.mock(InitiativeRestClient.class);
    restConnector = new InitiativeRestConnectorImpl(restClient);
  }

  @Test
  void getInitiativeBeneficiaryView_ok(){
    assertDoesNotThrow(() -> restConnector.getInitiativeBeneficiaryView(INITIATIVE_ID));
    verify(restClient).getInitiativeBeneficiaryView(INITIATIVE_ID);
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
              "rest-client.initiative.baseUrl=http://%s:%d",
              wireMockServer.getOptions().bindAddress(), wireMockServer.port()));
    }
  }
}
