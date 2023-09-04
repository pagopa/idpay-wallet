package it.gov.pagopa.wallet.exception;

import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@SuppressWarnings("squid:S110")
public class WalletException extends ClientExceptionWithBody {

  public WalletException(Integer code, String message) {
    super(HttpStatus.valueOf(code), code, message);
  }
}
