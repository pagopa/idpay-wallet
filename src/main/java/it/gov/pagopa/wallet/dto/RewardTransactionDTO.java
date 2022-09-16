package it.gov.pagopa.wallet.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@AllArgsConstructor
public class RewardTransactionDTO extends TransactionDTO {

  private String status;

  @Builder.Default
  private List<String> rejectionReasons = new ArrayList<>();

  @Builder.Default
  private Map<String, List<String>> initiativeRejectionReasons = new HashMap<>();

  private Map<String, RewardDTO> rewards;
}
