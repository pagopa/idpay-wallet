package it.gov.pagopa.wallet.controller.zendesk;

import it.gov.pagopa.wallet.dto.zendesk.SupportRequestDTO;
import it.gov.pagopa.wallet.dto.zendesk.SupportResponseDTO;
import it.gov.pagopa.wallet.service.zendesk.SupportService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SupportControllerTest {

    private MockMvc mockMvc;
    private SupportService supportService;

    @BeforeEach
    void setup() {
        supportService = mock(SupportService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SupportController(supportService))
                .build();
    }

    @Test
    void buildJwt_success_returns200WithBody_andPassesDTOToService() throws Exception {
        var reqJson = """
            {
              "email": "user@example.com",
              "firstName": "Mario",
              "lastName": "Rossi",
              "fiscalCode": "MRARSS...",
              "productId": "PROD123"
            }
            """;

        var expected = new SupportResponseDTO("jwt-token", "https://example/return?product=PROD123");
        when(supportService.buildJwtAndReturnTo(any(SupportRequestDTO.class))).thenReturn(expected);

        mockMvc.perform(post("/idpay/wallet/support")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.jwt", equalTo("jwt-token")))
                .andExpect(jsonPath("$.returnTo", equalTo("https://example/return?product=PROD123")));

        ArgumentCaptor<SupportRequestDTO> captor = ArgumentCaptor.forClass(SupportRequestDTO.class);
        verify(supportService, times(1)).buildJwtAndReturnTo(captor.capture());
        SupportRequestDTO dto = captor.getValue();

        assertEquals("user@example.com", dto.email());
        assertEquals("Mario", dto.firstName());
        assertEquals("Rossi", dto.lastName());
        assertEquals("MRARSS...", dto.fiscalCode());
        assertEquals("PROD123", dto.productId());
    }

    @Test
    void buildJwt_serviceThrows_propagatesException() {
        var reqJson = """
        {"email":"boom@example.com","firstName":"A","lastName":"B","fiscalCode":"X","productId":"Y"}
        """;

        when(supportService.buildJwtAndReturnTo(any()))
                .thenThrow(new RuntimeException("kaboom"));

        ServletException ex = assertThrows(
                ServletException.class,
                () -> mockMvc.perform(
                        MockMvcRequestBuilders.post("/idpay/wallet/support")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(reqJson)
                ).andReturn() // forza l'esecuzione e lascia propagare l'eccezione
        );

        // opzionale: verifica la causa e il messaggio
        assertNotNull(ex.getCause());
        assertEquals(RuntimeException.class, ex.getCause().getClass());
        assertEquals("kaboom", ex.getCause().getMessage());
    }
}
