package dev.is_a.tevarindol.bookstore.order;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
  private final OrderService orders;

  @GetMapping
  public String history(Authentication authentication, Model model) {
    model.addAttribute("orders", orders.forCustomer(authentication.getName()));
    return "orders/list";
  }
}
