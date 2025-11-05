package it.gov.pagopa.wallet.controller.zendesk;

import it.gov.pagopa.wallet.dto.zendesk.SupportRequestDTO;
import it.gov.pagopa.wallet.dto.zendesk.SupportResponseDTO;
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
        log.info("[ZENDESK-CONNECTOR-CONTROLLER] init");
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SupportResponseDTO> buildJwt(@RequestBody @Valid SupportRequestDTO body) {
        log.info("[ZENDESK-CONNECTOR-CONTROLLER] request for {}", body.email());
        SupportResponseDTO res = supportService.buildJwtAndReturnTo(body);
        return ResponseEntity.ok(res);
    }
}
