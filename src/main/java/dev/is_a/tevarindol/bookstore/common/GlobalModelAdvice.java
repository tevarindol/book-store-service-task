package dev.is_a.tevarindol.bookstore.common;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class GlobalModelAdvice {
  @ModelAttribute("signedIn")
  public boolean signedIn() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return Optional.ofNullable(auth)
        .map(a -> a.isAuthenticated() && !"anonymousUser".equals(a.getPrincipal()))
        .orElse(false);
  }

  @ModelAttribute("employee")
  public boolean employee() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return Optional.ofNullable(auth)
        .map(
            a ->
                a.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_EMPLOYEE")))
        .orElse(false);
  }
}
