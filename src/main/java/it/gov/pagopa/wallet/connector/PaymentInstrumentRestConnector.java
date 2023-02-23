package it.gov.pagopa.wallet.connector;

import it.gov.pagopa.wallet.dto.*;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface PaymentInstrumentRestConnector {

  void enrollInstrument(@RequestBody InstrumentCallBodyDTO body);

  void disableAllInstrument(@RequestBody UnsubscribeCallDTO body);
  
  void deleteInstrument(@RequestBody DeactivationBodyDTO body);

  void enrollInstrumentIssuer(InstrumentIssuerCallDTO body);

  InstrumentDetailDTO getInstrumentInitiativesDetail(@PathVariable("idWallet") String idWallet,
                                                     @PathVariable("userId") String userId);
}
