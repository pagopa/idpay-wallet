package it.gov.pagopa.wallet.dto.zendesk;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

public record SupportRequestDTO(
        @NotBlank @Email String email,
        @NotBlank String fiscalCode,
        String name,
        String productId,
        String data
) {}