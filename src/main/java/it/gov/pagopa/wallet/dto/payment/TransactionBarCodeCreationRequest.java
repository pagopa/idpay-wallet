package it.gov.pagopa.wallet.dto.payment;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionBarCodeCreationRequest {

    @NotBlank
    private String initiativeId;
    private Long voucherAmountCents;
}
