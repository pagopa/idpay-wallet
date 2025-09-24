package it.gov.pagopa.wallet.dto.payment;

import it.gov.pagopa.wallet.enums.SyncTrxStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;

@Data
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class TransactionBarCodeEnrichedResponse {
    private String id;
    private String trxCode;
    private String initiativeId;
    private String initiativeName;
    private OffsetDateTime trxDate;
    private OffsetDateTime trxEndDate;
    private SyncTrxStatus status;
    private Long trxExpirationSeconds;
    private Long residualBudgetCents;

}
