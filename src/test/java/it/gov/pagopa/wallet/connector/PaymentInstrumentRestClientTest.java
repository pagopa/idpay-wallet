package it.gov.pagopa.wallet.connector;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.mongodb.assertions.Assertions;
import it.gov.pagopa.wallet.config.WalletConfig;
import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.DeactivationBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentCallBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentIssuerCallDTO;
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
    initializers = PaymentInstrumentRestClientTest.WireMockInitializer.class,
    classes = {
      PaymentInstrumentRestConnectorImpl.class,
      WalletConfig.class,
      FeignAutoConfiguration.class,
      HttpMessageConvertersAutoConfiguration.class
    })
@TestPropertySource(
    locations = "classpath:application.yml",
    properties = {"spring.application.name=idpay-payment-instrument-integration-rest"})
class PaymentInstrumentRestClientTest {

  private static final String USER_ID = "USER_ID";
  private static final String INITIATIVE_ID = "INITIATIVE_ID";
  private static final String ID_WALLET = "TEST_ID_WALLET";
  private static final String INSTRUMENT_ID = "TEST_INSTRUMENT_ID";
  private static final String CHANNEL = "CHANNEL";

  @Autowired private PaymentInstrumentRestClient restClient;

  @Autowired private PaymentInstrumentRestConnector restConnector;

  @Test
  void enroll_instrument_test() {

    final InstrumentCallBodyDTO instrument =
        new InstrumentCallBodyDTO(USER_ID, INITIATIVE_ID, ID_WALLET, CHANNEL);

    try {
      restConnector.enrollInstrument(instrument);
    } catch (Exception e) {
      Assertions.fail();
    }
  }

  @Test
  void delete_instrument_test() {

    final DeactivationBodyDTO instrument =
        new DeactivationBodyDTO(USER_ID, INITIATIVE_ID, INSTRUMENT_ID);

    try {
      restConnector.deleteInstrument(instrument);
    } catch (Exception e) {
      Assertions.fail();
    }
  }

  @Test
  void delete_all_instrument_test() {

    final UnsubscribeCallDTO instrument =
        new UnsubscribeCallDTO(INITIATIVE_ID, USER_ID, LocalDateTime.now().toString());

    try {
      restConnector.disableAllInstrument(instrument);
    } catch (Exception e) {
      Assertions.fail();
    }
  }

  @Test
  void enroll_instrument_issuer_test() {

    final InstrumentIssuerCallDTO instrument =
        new InstrumentIssuerCallDTO(INITIATIVE_ID, USER_ID,"hpan", CHANNEL, "VISA", "VISA", "***");

    try {
      restConnector.enrollInstrumentIssuer(instrument);
    } catch (Exception e) {
      Assertions.fail();
    }
  }

  @Test
  void get_instrument_initiatives_detail_test() {
    try {
      restConnector.getInstrumentInitiativesDetail(ID_WALLET, USER_ID, WalletConstants.FILTER_INSTRUMENT_STATUS_LIST);
    } catch (Exception e) {
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
              "rest-client.payment.instrument.baseUrl=http://%s:%d",
              wireMockServer.getOptions().bindAddress(), wireMockServer.port()));
    }
  }
}
