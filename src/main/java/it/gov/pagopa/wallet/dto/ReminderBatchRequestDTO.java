package it.gov.pagopa.wallet.dto;

import it.gov.pagopa.wallet.constants.WalletConstants;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderBatchRequestDTO {

  @NotEmpty(message = WalletConstants.ERROR_MANDATORY_FIELD)
  private List<String> initiativeIds;


}

