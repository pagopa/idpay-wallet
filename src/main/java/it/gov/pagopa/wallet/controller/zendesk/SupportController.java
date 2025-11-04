package it.gov.pagopa.wallet.controller.zendesk;

import it.gov.pagopa.wallet.dto.zendesk.SupportRequestDTO;
import it.gov.pagopa.wallet.service.zendesk.SupportService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(value = "/idpay/wallet/support", produces = MediaType.APPLICATION_JSON_VALUE)
public class SupportController {

    private final SupportService supportService;

    public SupportController(SupportService supportService) {
        this.supportService = supportService;
        log.info("[ZENDESK-CONNECTOR-CONTROLLER] - SupportController initialized");
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> sso(@RequestBody @Valid SupportRequestDTO body) {
        log.info("[ZENDESK-CONNECTOR-CONTROLLER] - Received SSO support request for email '{}'", body.email());
        log.info("[ZENDESK-CONNECTOR-CONTROLLER] - Starting SSO HTML generation");

        String html = supportService.buildSsoHtml(body);

        log.info("[ZENDESK-CONNECTOR-CONTROLLER] - Successfully generated SSO HTML for '{}'", body.email());
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }
}
