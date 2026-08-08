package it.gov.pagopa.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class RewardTransactionDTO extends TransactionDTO {

  private String status;

  private String initiativeId;

  private String initiativeName;

  @Builder.Default
  private List<String> rejectionReasons = new ArrayList<>();

  @Builder.Default
  private Map<String, List<String>> initiativeRejectionReasons = new HashMap<>();

  private Map<String, RewardDTO> rewards;
  private Instant elaborationDateTime;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private Instant trxChargeDate;

  private Boolean extendedAuthorization;

}
