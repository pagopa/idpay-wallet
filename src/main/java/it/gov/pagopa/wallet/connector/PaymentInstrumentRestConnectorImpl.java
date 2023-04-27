package it.gov.pagopa.wallet.connector;

import it.gov.pagopa.wallet.dto.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentInstrumentRestConnectorImpl implements PaymentInstrumentRestConnector {

  private final PaymentInstrumentRestClient paymentInstrumentRestClient;

  public PaymentInstrumentRestConnectorImpl(
      PaymentInstrumentRestClient paymentInstrumentRestClient) {
    this.paymentInstrumentRestClient = paymentInstrumentRestClient;
  }

  @Override
  public void enrollInstrument(InstrumentCallBodyDTO body) {
    paymentInstrumentRestClient.enrollInstrument(body);
  }

  @Override
  public void disableAllInstrument(UnsubscribeCallDTO body) {
    paymentInstrumentRestClient.disableAllInstrument(body);
  }

  @Override
  public void deleteInstrument(DeactivationBodyDTO body) {
    paymentInstrumentRestClient.deleteInstrument(body);
  }

  @Override
  public void enrollInstrumentIssuer(InstrumentIssuerCallDTO body) {
    paymentInstrumentRestClient.enrollInstrumentIssuer(body);
  }
  @Override
  public InstrumentDetailDTO getInstrumentInitiativesDetail(String idWallet, String userId, List<String> statusList){
    return paymentInstrumentRestClient.getInstrumentInitiativesDetail(idWallet, userId, statusList);
  }

  @Override
  public void enrollDiscountInitiative(InstrumentFromDiscountDTO body) {
    paymentInstrumentRestClient.enrollDiscountInitiative(body);
  }

}
