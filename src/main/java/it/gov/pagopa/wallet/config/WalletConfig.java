package it.gov.pagopa.wallet.config;

import it.gov.pagopa.wallet.connector.PaymentInstrumentRestClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(clients = {PaymentInstrumentRestClient.class})
public class WalletConfig {}
