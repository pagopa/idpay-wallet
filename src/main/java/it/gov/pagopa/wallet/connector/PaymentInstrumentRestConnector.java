package it.gov.pagopa.wallet.connector;

import it.gov.pagopa.wallet.dto.*;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface PaymentInstrumentRestConnector {

  void enrollInstrument(@RequestBody InstrumentCallBodyDTO body);

  void disableAllInstrument(@RequestBody UnsubscribeCallDTO body);
  
  void deleteInstrument(@RequestBody DeactivationBodyDTO body);

  void enrollInstrumentIssuer(InstrumentIssuerCallDTO body);

  InstrumentDetailDTO getInstrumentInitiativesDetail(@PathVariable("idWallet") String idWallet,
                                                     @PathVariable("userId") String userId,
                                                     @RequestParam("statusList") List<String> statusList);
  void enrollDiscountInitiative(@RequestBody InstrumentFromDiscountDTO body);
  void rollback(@PathVariable String initiativeId,@PathVariable String userId);
  void enrollInstrumentCode(@RequestBody InstrumentCallBodyDTO body);

  CheckEnrollmentDTO codeStatus(@PathVariable("userId") String userId);
}
