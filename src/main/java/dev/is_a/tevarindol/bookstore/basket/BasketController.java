package dev.is_a.tevarindol.bookstore.basket;

import dev.is_a.tevarindol.bookstore.book.Book;
import dev.is_a.tevarindol.bookstore.book.BookService;
import dev.is_a.tevarindol.bookstore.order.OrderDTO;
import dev.is_a.tevarindol.bookstore.order.OrderService;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class BasketController {
  private final BasketService basket;
  private final BookService books;
  private final OrderService orders;

  @GetMapping("/basket")
  public String view(HttpSession session, Model model) {
    var lines = new ArrayList<BasketLine>();
    BigDecimal total = BigDecimal.ZERO;
    for (var entry : basket.get(session).entrySet()) {
      var book = books.get(entry.getKey());
      var line = new BasketLine(book, entry.getValue());
      lines.add(line);
      total = total.add(line.subtotal());
    }
    model.addAttribute("lines", lines);
    model.addAttribute("total", total);
    return "basket/view";
  }

  @PostMapping("/basket/{id}")
  public String add(@PathVariable long id, HttpSession session, RedirectAttributes redirect) {
    var book = books.get(id);
    if (book.getStockQuantity() < basket.get(session).getOrDefault(id, 0) + 1) {
      redirect.addFlashAttribute("error", "basket.insufficientStock");
    } else {
      basket.add(session, id);
      redirect.addFlashAttribute("notice", "book.addedToBasket");
    }
    return "redirect:/basket";
  }

  @PostMapping("/basket/{id}/remove")
  public String remove(@PathVariable long id, HttpSession session) {
    basket.remove(session, id);
    return "redirect:/basket";
  }

  @PostMapping("/checkout")
  public String checkout(
      HttpSession session, Authentication authentication, RedirectAttributes redirect) {
    var orderDto =
        new OrderDTO(
            basket.get(session).entrySet().stream()
                .map(entry -> new OrderDTO.OrderLineDTO(entry.getKey(), entry.getValue()))
                .toList());
    orders.addOrder(authentication.getName(), orderDto);
    basket.clear(session);
    redirect.addFlashAttribute("notice", "order.submitted");
    return "redirect:/orders";
  }

  public record BasketLine(Book book, int quantity) {
    public BigDecimal subtotal() {
      return book.getPrice().multiply(BigDecimal.valueOf(quantity));
    }
  }
}
