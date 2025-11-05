package it.gov.pagopa.wallet.dto.zendesk;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

public record SupportRequestDTO(
        @NotBlank @Email String email,
        String firstName,
        String lastName,
        String fiscalCode,
        String productId
) {}