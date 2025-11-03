package it.gov.pagopa.wallet.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

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
  private LocalDateTime elaborationDateTime;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime trxChargeDate;

  private Boolean extendedAuthorization;

}
