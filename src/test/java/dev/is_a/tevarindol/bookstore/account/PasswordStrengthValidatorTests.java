package dev.is_a.tevarindol.bookstore.account;

import static org.assertj.core.api.Assertions.assertThat;

import dev.is_a.tevarindol.bookstore.config.SecurityProperties;
import org.junit.jupiter.api.Test;

class PasswordStrengthValidatorTests {
  private final PasswordStrengthValidator validator =
      new PasswordStrengthValidator(new SecurityProperties(12, 3));

  @Test
  void acceptsNullPassword() {
    assertThat(validator.isValid(null, null)).isTrue();
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
  void rejectsPredictablePasswordBelowConfiguredScore() {
    assertThat(validator.isValid("Password123", null)).isFalse();
  }
}
