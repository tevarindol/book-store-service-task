package dev.is_a.tevarindol.bookstore.account;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {
  private final AccountService accounts;
  private final ModelMapper mapper;

  @GetMapping
  public String profile(Authentication authentication, Model model) {
    var account = accounts.getByEmail(authentication.getName());
    ProfileForm form = mapper.map(account, ProfileForm.class);
    model.addAttribute("account", account);
    model.addAttribute("profileForm", form);
    return "account/profile";
  }

  @PostMapping
  public String update(
      @Valid ProfileForm profileForm,
      BindingResult result,
      Authentication authentication,
      Model model) {
    if (result.hasErrors()) {
      model.addAttribute("account", accounts.getByEmail(authentication.getName()));
      return "account/profile";
    }
    accounts.updateProfileFields(
        authentication.getName(),
        profileForm.getFirstName(),
        profileForm.getLastName(),
        profileForm.getPhone());
    return "redirect:/profile?updated";
  }

  @PostMapping("/delete")
  public String delete(
      Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
    accounts.deleteCustomer(authentication.getName());
    new SecurityContextLogoutHandler().logout(request, response, authentication);
    return "redirect:/books?accountDeleted";
  }
}
