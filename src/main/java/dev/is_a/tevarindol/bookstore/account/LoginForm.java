package dev.is_a.tevarindol.bookstore.account;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginForm {
  @NotBlank private String username;
  @NotBlank private String password;
}
