package it.gov.pagopa.wallet.dto.zendesk;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SupportRequestDTO(
        @NotBlank(message = "Email mancante")
        @Pattern(
                regexp = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$",
                message = "Email non valida"
        )
        String email,
        String firstName,
        String lastName,
        String fiscalCode,
        String productId
) {}