package dev.is_a.tevarindol.bookstore.account;

import com.nulabinc.zxcvbn.Zxcvbn;
import dev.is_a.tevarindol.bookstore.config.SecurityProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordStrengthValidator implements ConstraintValidator<PasswordStrength, String> {
  private final Zxcvbn zxcvbn = new Zxcvbn();
  private final int minScore;

  public PasswordStrengthValidator(SecurityProperties properties) {
    this.minScore = properties.passwordMinScore();
  }

  @Override
  public boolean isValid(String password, ConstraintValidatorContext context) {
    if (password == null || password.isBlank()) return true;

    return zxcvbn.measure(password).getScore() >= minScore;
  }
}
