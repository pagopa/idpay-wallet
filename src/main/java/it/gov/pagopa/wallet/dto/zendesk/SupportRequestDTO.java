package it.gov.pagopa.wallet.dto.zendesk;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

import java.util.Map;

public record SupportRequestDTO(
        @NotBlank @Email
        String email,

        @NotBlank
        String fiscalCode,

        String name,

        String ticketFormId,

        String subject,

        String message,

        String productId,

        String data,

        Map<String, String> customFields
) {}