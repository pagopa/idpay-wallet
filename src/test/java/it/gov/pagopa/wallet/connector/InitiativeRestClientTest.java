package it.gov.pagopa.wallet.connector;

import it.gov.pagopa.wallet.dto.initiative.InitiativeDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class InitiativeRestClientTest {

  private static final String INITIATIVE_ID = "INITIATIVE_ID";

  private InitiativeRestClient restClient;
  private InitiativeRestConnectorImpl restConnector;

  @BeforeEach
  void setUp() {
    restClient = Mockito.mock(InitiativeRestClient.class);
    restConnector = new InitiativeRestConnectorImpl(restClient);
  }


  @Test
  void getInitiativeBeneficiaryView(){
    InitiativeDTO initiativeDTO = new InitiativeDTO();
    when(restClient.getInitiativeBeneficiaryView(any())).thenReturn(initiativeDTO);
    assertNotNull(restConnector.getInitiativeBeneficiaryView("test"));
    verify(restClient).getInitiativeBeneficiaryView(any());

  }

}
