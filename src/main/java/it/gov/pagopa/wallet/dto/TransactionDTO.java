package it.gov.pagopa.wallet.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.gov.pagopa.wallet.utils.json.BigDecimalScale2Deserializer;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDTO {
  private String id;

  private String idTrxAcquirer;

  private String acquirerCode;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime trxDate;

  private String brandLogo;

  private String brand;

  private String maskedPan;

  private String instrumentId;

  private String operationType;

  private String circuitType;

  private String idTrxIssuer;

  private String correlationId;

  @JsonDeserialize(using = BigDecimalScale2Deserializer.class)
  private BigDecimal amount;

  private String amountCurrency;

  private String mcc;

  private String acquirerId;

  private String merchantId;

  private String terminalId;

  private String bin;

  private String senderCode;

  private String fiscalCode;

  private String vat;

  private String posType;

  private String par;

  private String userId;

  private BigDecimal effectiveAmount;

  private String channel;

  private String businessName;
}
