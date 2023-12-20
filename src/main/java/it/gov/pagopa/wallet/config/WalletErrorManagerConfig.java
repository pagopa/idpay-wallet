package it.gov.pagopa.wallet.config;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.wallet.constants.WalletConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WalletErrorManagerConfig {
    @Bean
    ErrorDTO defaultErrorDTO() {
        return new ErrorDTO(
                WalletConstants.ExceptionCode.GENERIC_ERROR,
                "A generic error occurred"
        );
    }

    @Bean
    ErrorDTO tooManyRequestsErrorDTO() {
        return new ErrorDTO(WalletConstants.ExceptionCode.TOO_MANY_REQUESTS, "Too Many Requests");
    }

    @Bean
    ErrorDTO templateValidationErrorDTO(){
        return new ErrorDTO(WalletConstants.ExceptionCode.INVALID_REQUEST, null);
    }
}
