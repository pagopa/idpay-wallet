package it.gov.pagopa.wallet.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class WalletException extends RuntimeException {

  private final int code;

  private final String message;

}
