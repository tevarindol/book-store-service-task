package dev.is_a.tevarindol.bookstore.account;

import dev.is_a.tevarindol.bookstore.config.JwtCookieService;
import dev.is_a.tevarindol.bookstore.config.JwtTokenService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class LoginController {
  private final AuthenticationManager authenticationManager;
  private final JwtTokenService tokens;
  private final JwtCookieService cookies;

  @PostMapping("/login")
  public String login(
      @Valid @ModelAttribute LoginForm form, BindingResult result, HttpServletResponse response) {
    if (result.hasErrors()) return "redirect:/login?error";

    try {
      Authentication authentication =
          authenticationManager.authenticate(
              UsernamePasswordAuthenticationToken.unauthenticated(
                  form.getUsername(), form.getPassword()));
      cookies.writeToken(response, tokens.issue(authentication));
      return "redirect:/books";
    } catch (AuthenticationException exception) {
      cookies.clear(response);
      return "redirect:/login?error";
    }
  }
}
