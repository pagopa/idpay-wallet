package it.gov.pagopa.wallet.dto.zendesk;

public record SupportResponseDTO(
        String jwt,
        String returnTo
) {}
