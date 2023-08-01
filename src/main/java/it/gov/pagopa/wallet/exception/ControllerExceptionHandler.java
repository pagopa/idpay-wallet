package it.gov.pagopa.wallet.exception;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import org.iban4j.IbanFormatException;
import org.iban4j.InvalidCheckDigitException;
import org.iban4j.UnsupportedCountryException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class ControllerExceptionHandler {


  @ExceptionHandler({WalletException.class})
  public ResponseEntity<ErrorDTO> handleException(WalletException ex) {
    return new ResponseEntity<>(new ErrorDTO(ex.getCode(), ex.getMessage(),""),
        HttpStatus.valueOf(ex.getCode()));
  }

  @ExceptionHandler({InvalidCheckDigitException.class})
  public ResponseEntity<ErrorDTO> handleInvalidCheckDigitException(InvalidCheckDigitException ex) {
    return new ResponseEntity<>(new ErrorDTO(400, ex.getMessage(),""),
        HttpStatus.valueOf(400));
  }

  @ExceptionHandler({IbanFormatException.class})
  public ResponseEntity<ErrorDTO> handleIbanFormatException(IbanFormatException ex) {
    return new ResponseEntity<>(new ErrorDTO(400, ex.getMessage(),""),
        HttpStatus.valueOf(400));
  }

  @ExceptionHandler({UnsupportedCountryException.class})
  public ResponseEntity<ErrorDTO> handleUnsupportedCountryException(UnsupportedCountryException ex) {
    return new ResponseEntity<>(new ErrorDTO(400, ex.getMessage(),""),
        HttpStatus.valueOf(400));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorDTO> handleValidationExceptions(
      MethodArgumentNotValidException ex) {
    List<String> errors = new ArrayList<>();
    ex.getBindingResult().getAllErrors().forEach(error -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      errors.add(String.format("[%s]: %s", fieldName, errorMessage));
    });
    String message = String.join(" - ", errors);
    return new ResponseEntity<>(
        new ErrorDTO(HttpStatus.BAD_REQUEST.value(), message,""),
        HttpStatus.BAD_REQUEST);
  }
}
