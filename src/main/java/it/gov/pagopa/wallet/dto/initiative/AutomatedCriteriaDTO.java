package it.gov.pagopa.wallet.dto.initiative;

import lombok.Data;

@Data
public class AutomatedCriteriaDTO   {

  private String authority;

  private String code;

  private String field;

  private String operator;

  private String value;

  private String value2;
}
