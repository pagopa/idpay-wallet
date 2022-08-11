package it.gov.pagopa.wallet.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Document(collection = "wallet")
@CompoundIndex(name = "wallet_unique_idx", def = "{'userId': 1, 'initiativeId': 1}", unique = true)
public class Wallet {

  @Id private String id;
  private String userId;
  private String initiativeId;
  private String initiativeName;
  private String status;
  private LocalDateTime acceptanceDate;
  private LocalDateTime endDate;
  private String iban;
  private String email;
  private LocalDateTime emailUpdate;
  private BigDecimal amount;
  private BigDecimal accrued;
  private BigDecimal refunded;
  private int nTrx;
  private int nInstr;
}
