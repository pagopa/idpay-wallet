package it.gov.pagopa.common.web.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ClientExceptionWithBody extends ClientException{
  private final Integer code;

  public ClientExceptionWithBody(HttpStatus httpStatus, Integer code, String message){
    this(httpStatus, code, message,null);
  }

  public ClientExceptionWithBody(HttpStatus httpStatus, Integer code, String message, Throwable ex){
    this(httpStatus, code, message, false, ex);
  }

  public ClientExceptionWithBody(HttpStatus httpStatus, Integer code, String message, boolean printStackTrace, Throwable ex){
    super(httpStatus, message, printStackTrace, ex);
    this.code = code;
  }
}
