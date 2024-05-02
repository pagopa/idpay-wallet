package it.gov.pagopa.wallet.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Counters {
  private boolean exhaustedBudget;
  private Long trxNumber;
  private Long totalRewardCents;
  private Long initiativeBudgetCents;
  private Long totalAmountCents;
  private Long version;
}
