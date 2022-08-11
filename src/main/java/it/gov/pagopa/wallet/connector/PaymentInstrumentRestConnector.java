package it.gov.pagopa.wallet.connector;

import it.gov.pagopa.wallet.dto.InstrumentCallBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentResponseDTO;
import org.springframework.web.bind.annotation.RequestBody;

public interface PaymentInstrumentRestConnector {

  InstrumentResponseDTO enrollInstrument(@RequestBody InstrumentCallBodyDTO body);
}
