package it.gov.pagopa.wallet.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class WalletDTO {

  private String initiativeId;

  private String initiativeName;

  private String status;

  private String iban;

  private LocalDate endDate;

  @JsonProperty("nInstr")
  private int nInstr;

  private BigDecimal amount;

  private BigDecimal accrued;

  private BigDecimal refunded;
  private LocalDateTime lastCounterUpdate;

}
