package it.gov.pagopa.wallet.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
  private String organizationId;
  private String status;
  private LocalDateTime acceptanceDate;
  private LocalDateTime endDate;
  private String iban;
  private BigDecimal amount;
  private BigDecimal accrued;
  private BigDecimal refunded;
  private Map<String, RefundHistory> refundHistory;
  private Long nTrx;
  private int nInstr;
  private LocalDateTime requestUnsubscribeDate;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RefundHistory{
    private Long feedbackProgressive;
  }
}
