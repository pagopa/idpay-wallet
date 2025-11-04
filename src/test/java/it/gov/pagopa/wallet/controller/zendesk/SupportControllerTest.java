package it.gov.pagopa.wallet.controller.zendesk;

import it.gov.pagopa.wallet.dto.zendesk.SupportRequestDTO;
import it.gov.pagopa.wallet.service.zendesk.SupportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SupportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SupportService supportService;

    private SupportController controller;

    private static final String BASE_URL = "/idpay/wallet/support";

    @RestControllerAdvice
    static class TestGlobalExceptionHandler {

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<String> handleBadJson(HttpMessageNotReadableException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("bad request");
        }

        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<String> handleRuntime(RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("internal error");
        }
    }

    @BeforeEach
    void setUp() {
        controller = new SupportController(supportService);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new TestGlobalExceptionHandler())
                .build();
    }

    @Test
    void sso_returnsHtmlFromService_andTextHtmlContentType() throws Exception {
        String expectedHtml = "<html>ok</html>";
        when(supportService.buildSsoHtml(any(SupportRequestDTO.class))).thenReturn(expectedHtml);

        String requestJson = """
            {
              "email": "user@example.org",
              "name": "Mario Rossi",
              "fiscalCode": "ABCDEF12G34H567I",
              "ticketFormId": "999999",
              "subject": "subject",
              "message": "message",
              "productId": "PRD-123",
              "data": "meta",
              "customFields": {"1001": "TAG_A"}
            }
            """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<html>ok</html>")));

        ArgumentCaptor<SupportRequestDTO> captor = ArgumentCaptor.forClass(SupportRequestDTO.class);
        verify(supportService, times(1)).buildSsoHtml(captor.capture());
        SupportRequestDTO passed = captor.getValue();
        assertEquals("user@example.org", passed.email());
        assertEquals("Mario Rossi", passed.name());
        assertEquals("ABCDEF12G34H567I", passed.fiscalCode());
    }

    @Test
    void sso_whenServiceThrows_returns500() throws Exception {
        when(supportService.buildSsoHtml(any(SupportRequestDTO.class)))
                .thenThrow(new RuntimeException("boom"));

        String requestJson = """
            {
              "email": "user@example.org",
              "name": "Mario Rossi",
              "fiscalCode": "ABCDEF12G34H567I"
            }
            """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("internal error"));

        verify(supportService, times(1)).buildSsoHtml(any(SupportRequestDTO.class));
    }

    @Test
    void sso_withInvalidJson_returns400() throws Exception {
        String badJson = """
            {
              "email": "user@example.org"
              "fiscalCode": "ABCDEF12G34H567I"
            }
            """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("bad request"));

        verify(supportService, never()).buildSsoHtml(any());
    }
}
