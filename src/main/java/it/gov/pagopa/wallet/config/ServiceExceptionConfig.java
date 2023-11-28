package it.gov.pagopa.wallet.config;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.wallet.exception.custom.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ServiceExceptionConfig {

    @Bean
    public Map<Class<? extends ServiceException>, HttpStatus> serviceExceptionMapper() {
        Map<Class<? extends ServiceException>, HttpStatus> exceptionMap = new HashMap<>();

        // BadRequest
        exceptionMap.put(OperationNotAllowedException.class, HttpStatus.BAD_REQUEST);

        // Forbidden
        exceptionMap.put(EnrollmentNotAllowedException.class, HttpStatus.FORBIDDEN);
        exceptionMap.put(InitiativeInvalidException.class, HttpStatus.FORBIDDEN);
        exceptionMap.put(UserUnsubscribedException.class, HttpStatus.FORBIDDEN);
        exceptionMap.put(InvalidIbanException.class, HttpStatus.FORBIDDEN);
        exceptionMap.put(UserNotAllowedException.class, HttpStatus.FORBIDDEN);
        exceptionMap.put(InstrumentDeleteNotAllowedException.class, HttpStatus.FORBIDDEN);
        exceptionMap.put(IdPayCodeNotEnabledException.class, HttpStatus.FORBIDDEN);

        // NotFound
        exceptionMap.put(PaymentInstrumentNotFoundException.class, HttpStatus.NOT_FOUND);
        exceptionMap.put(UserNotOnboardedException.class, HttpStatus.NOT_FOUND);

        // InternalServerError
        exceptionMap.put(OnboardingInvocationException.class, HttpStatus.INTERNAL_SERVER_ERROR);
        exceptionMap.put(PaymentInstrumentInvocationException.class, HttpStatus.INTERNAL_SERVER_ERROR);

        return exceptionMap;
    }
}
