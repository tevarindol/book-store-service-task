package dev.is_a.tevarindol.bookstore.common;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
public class ErrorPageController implements ErrorController {
  private final MessageSource messages;

  @RequestMapping("/error")
  public String error(HttpServletRequest request, Model model, Locale locale) {
    Object statusAttribute = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    int status = statusAttribute instanceof Integer value ? value : 500;
    model.addAttribute("status", status);
    model.addAttribute("message", messages.getMessage(messageCodeFor(status), null, locale));
    return "error/error";
  }

  @RequestMapping("/error/403")
  public String forbidden(Model model, Locale locale) {
    model.addAttribute("status", 403);
    model.addAttribute("message", messages.getMessage("error.forbidden", null, locale));
    return "error/error";
  }

  private String messageCodeFor(int status) {
    return switch (status) {
      case 400 -> "error.badRequest";
      case 403 -> "error.forbidden";
      case 404 -> "error.notFound";
      default -> "error.unexpected";
    };
  }
}
