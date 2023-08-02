package it.gov.pagopa.wallet.exception;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import org.iban4j.IbanFormatException;
import org.iban4j.InvalidCheckDigitException;
import org.iban4j.UnsupportedCountryException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ControllerExceptionHandler {

  @ExceptionHandler({InvalidCheckDigitException.class})
  public ResponseEntity<ErrorDTO> handleInvalidCheckDigitException(InvalidCheckDigitException ex) {
    return new ResponseEntity<>(new ErrorDTO(400, ex.getMessage()),
        HttpStatus.valueOf(400));
  }

  @ExceptionHandler({IbanFormatException.class})
  public ResponseEntity<ErrorDTO> handleIbanFormatException(IbanFormatException ex) {
    return new ResponseEntity<>(new ErrorDTO(400, ex.getMessage()),
        HttpStatus.valueOf(400));
  }

  @ExceptionHandler({UnsupportedCountryException.class})
  public ResponseEntity<ErrorDTO> handleUnsupportedCountryException(UnsupportedCountryException ex) {
    return new ResponseEntity<>(new ErrorDTO(400, ex.getMessage()),
        HttpStatus.valueOf(400));
  }

}
