package it.gov.pagopa.wallet.connector;

import it.gov.pagopa.wallet.dto.DeactivationBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentCallBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentIssuerDTO;
import it.gov.pagopa.wallet.dto.UnsubscribeCallDTO;
import org.springframework.web.bind.annotation.RequestBody;

public interface PaymentInstrumentRestConnector {

  void enrollInstrument(@RequestBody InstrumentCallBodyDTO body);

  void disableAllInstrument(@RequestBody UnsubscribeCallDTO body);
  
  void deleteInstrument(@RequestBody DeactivationBodyDTO body);

  void enrollInstrumentIssuer(InstrumentIssuerDTO body);
}
