package it.gov.pagopa.wallet.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class WalletUpdateException extends RuntimeException {

    private final String message;
}
