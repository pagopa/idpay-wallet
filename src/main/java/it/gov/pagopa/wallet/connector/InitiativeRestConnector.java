package it.gov.pagopa.wallet.connector;

import it.gov.pagopa.wallet.dto.initiative.InitiativeDTO;
import org.springframework.web.bind.annotation.PathVariable;

public interface InitiativeRestConnector {
  InitiativeDTO getInitiativeBeneficiaryView(@PathVariable("initiativeId") String initiativeId);
}
