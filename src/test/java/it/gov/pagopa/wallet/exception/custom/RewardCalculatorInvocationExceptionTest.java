package it.gov.pagopa.wallet.exception.custom;

import org.junit.jupiter.api.Test;

import static it.gov.pagopa.wallet.constants.WalletConstants.ExceptionCode.GENERIC_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class RewardCalculatorInvocationExceptionTest {

  @Test
  void constructor_withMessage_shouldSetGenericCodeAndMessage() {
    RewardCalculatorInvocationException exception =
        new RewardCalculatorInvocationException("test-message");

    assertEquals(GENERIC_ERROR, exception.getCode());
    assertEquals("test-message", exception.getMessage());
  }

  @Test
  void constructor_withMessageAndCause_shouldSetGenericCodeMessageAndCause() {
    RuntimeException cause = new RuntimeException("root-cause");

    RewardCalculatorInvocationException exception =
        new RewardCalculatorInvocationException("test-message", true, cause);

    assertEquals(GENERIC_ERROR, exception.getCode());
    assertEquals("test-message", exception.getMessage());
    assertSame(cause, exception.getCause());
  }

  @Test
  void constructor_withCodeAndMessage_shouldSetProvidedValues() {
    RewardCalculatorInvocationException exception =
        new RewardCalculatorInvocationException("CUSTOM_CODE", "custom-message");

    assertEquals("CUSTOM_CODE", exception.getCode());
    assertEquals("custom-message", exception.getMessage());
  }

  @Test
  void constructor_full_shouldSetProvidedValuesAndCause() {
    RuntimeException cause = new RuntimeException("root-cause");

    RewardCalculatorInvocationException exception =
        new RewardCalculatorInvocationException("CUSTOM_CODE", "custom-message", null, true,
            cause);

    assertEquals("CUSTOM_CODE", exception.getCode());
    assertEquals("custom-message", exception.getMessage());
    assertSame(cause, exception.getCause());
  }
}
