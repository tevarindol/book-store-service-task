package dev.is_a.tevarindol.bookstore.account;

import dev.is_a.tevarindol.bookstore.common.ConflictException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class RegistrationController {
  private final AccountService accounts;

  @GetMapping("/login")
  public String login() {
    return "auth/login";
  }

  @GetMapping("/register")
  public String registerForm(Model model) {
    if (!model.containsAttribute("accountForm"))
      model.addAttribute("accountForm", new AccountForm());
    return "auth/register";
  }

  @PostMapping("/register")
  public String register(
      @Valid @ModelAttribute AccountForm accountForm,
      BindingResult result,
      RedirectAttributes redirect) {
    if (result.hasErrors()) return "auth/register";
    try {
      accounts.register(accountForm);
    } catch (ConflictException e) {
      result.rejectValue("email", e.getMessage());
      return "auth/register";
    }
    redirect.addFlashAttribute("notice", "account.created");
    return "redirect:/login";
  }
}
