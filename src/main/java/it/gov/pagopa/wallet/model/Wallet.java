package it.gov.pagopa.wallet.model;

import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "wallet")
@CompoundIndex(name = "wallet_unique_idx", def = "{'userId': 1, 'initiativeId': 1}", unique = true)
public class Wallet {

  public Wallet(
      String userId,
      String initiativeId,
      String initiativeName,
      String status,
      LocalDateTime acceptanceDate,
      LocalDateTime endDate,
      double amount) {
    this.userId = userId;
    this.initiativeId = initiativeId;
    this.initiativeName = initiativeName;
    this.status = status;
    this.acceptanceDate = acceptanceDate;
    this.endDate = endDate;
    this.amount = amount;
  }

  @Id
  private String id;

  private String userId;

  private String initiativeId;

  private String initiativeName;

  private String status;

  private String iban;

  private LocalDateTime acceptanceDate;

  private LocalDateTime endDate;

  private int nTrx;

  private int nInstr;

  private double amount;

  private double accrued;

  private double refunded;
}
