package it.gov.pagopa.wallet.connector;

import it.gov.pagopa.wallet.dto.DeactivationBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentCallBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentResponseDTO;
import it.gov.pagopa.wallet.dto.UnsubscribeCallDTO;
import org.springframework.stereotype.Service;

@Service
public class PaymentInstrumentRestConnectorImpl implements PaymentInstrumentRestConnector {

  private final PaymentInstrumentRestClient paymentInstrumentRestClient;

  public PaymentInstrumentRestConnectorImpl(
      PaymentInstrumentRestClient paymentInstrumentRestClient) {
    this.paymentInstrumentRestClient = paymentInstrumentRestClient;
  }

  @Override
  public InstrumentResponseDTO enrollInstrument(InstrumentCallBodyDTO body) {
    return paymentInstrumentRestClient.enrollInstrument(body);
  }

  @Override
  public void disableAllInstrument(UnsubscribeCallDTO body) {
    paymentInstrumentRestClient.disableAllInstrument(body);
  }

  @Override
  public InstrumentResponseDTO deleteInstrument(DeactivationBodyDTO body) {
    return paymentInstrumentRestClient.deleteInstrument(body);
  }

}
