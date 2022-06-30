package it.gov.pagopa.wallet.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.wallet.dto.IbanCallBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentCallBodyDTO;
import it.gov.pagopa.wallet.dto.InstrumentResponseDTO;

public interface WalletRestService {
  InstrumentResponseDTO callPaymentInstrument(InstrumentCallBodyDTO dto)
      throws JsonProcessingException;

  void callIban(IbanCallBodyDTO dto)
          throws JsonProcessingException;
}
