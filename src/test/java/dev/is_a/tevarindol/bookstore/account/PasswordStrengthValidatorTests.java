package dev.is_a.tevarindol.bookstore.account;

import static org.assertj.core.api.Assertions.assertThat;

import dev.is_a.tevarindol.bookstore.config.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordStrengthValidatorTests {
  private final PasswordStrengthValidator validator =
      new PasswordStrengthValidator(new SecurityProperties(12, 3));

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void leavesMissingAndBlankPasswordsToNotBlank(String password) {
    assertThat(validator.isValid(password, null)).isTrue();
  }

  @Test
  void rejectsShortPasswordBelowConfiguredScore() {
    assertThat(validator.isValid("abcdefghij", null)).isFalse();
  }

  @Test
  void acceptsLongPassphraseAtConfiguredScore() {
    assertThat(validator.isValid("vivid lunar orchard velvet compass 47", null)).isTrue();
  }

  @Test
  void acceptsScoreThreeAtThresholdThreeButRejectsItAtFour() {
    String password = "vivid lunar";
    var stricterValidator = new PasswordStrengthValidator(new SecurityProperties(12, 4));

    assertThat(validator.isValid(password, null)).isTrue();
    assertThat(stricterValidator.isValid(password, null)).isFalse();
  }

  @Test
  void rejectsPredictablePasswordBelowConfiguredScore() {
    assertThat(validator.isValid("Password123", null)).isFalse();
  }
}
