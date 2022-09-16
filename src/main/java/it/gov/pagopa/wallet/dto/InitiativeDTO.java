package it.gov.pagopa.wallet.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiativeDTO {

  private String initiativeId;

  private String initiativeName;

  private String status;

  private String iban;

  private LocalDateTime endDate;

  private String nInstr;

  private BigDecimal amount;

  private BigDecimal accrued;

  private BigDecimal refunded;

}
