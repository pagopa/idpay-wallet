package it.gov.pagopa.wallet.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder(toBuilder = true)
@Document(collection = "wallet")
@FieldNameConstants
public class Wallet {

  @Id private String id;
  private String userId;
  private String familyId;
  private String initiativeId;
  private String initiativeName;
  private String organizationId;
  private String organizationName;
  private String status;
  private String voucherStatus;
  private LocalDateTime acceptanceDate;
  private LocalDate initiativeEndDate;
  private LocalDate voucherStartDate;
  private LocalDate voucherEndDate;
  private String iban;
  private Long amountCents;
  private Long accruedCents;
  private Long refundedCents;
  private Map<String, RefundHistory> refundHistory;
  private Long nTrx;
  private int nInstr;
  private LocalDateTime requestUnsubscribeDate;
  private LocalDateTime updateDate;
  private LocalDateTime lastCounterUpdate;
  private LocalDateTime suspensionDate;
  private String initiativeRewardType;
  private Boolean isLogoPresent;
  private Long maxTrx;
  private Long counterVersion;
  private List<Long> counterHistory;
  private String serviceId;
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RefundHistory{
    private Long feedbackProgressive;
  }
}
