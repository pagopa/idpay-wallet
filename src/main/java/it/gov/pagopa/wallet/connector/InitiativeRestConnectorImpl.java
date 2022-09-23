package it.gov.pagopa.wallet.connector;

import it.gov.pagopa.wallet.dto.initiative.InitiativeDTO;
import org.springframework.stereotype.Service;

@Service
public class InitiativeRestConnectorImpl implements InitiativeRestConnector {

  private final InitiativeRestClient initiativeRestClient;

  public InitiativeRestConnectorImpl(
      InitiativeRestClient initiativeRestClient) {
    this.initiativeRestClient = initiativeRestClient;
  }

  @Override
  public InitiativeDTO getInitiativeBeneficiaryView(String initiativeId) {
    return initiativeRestClient.getInitiativeBeneficiaryView(initiativeId);
  }
}
