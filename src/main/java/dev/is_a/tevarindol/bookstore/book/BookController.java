package dev.is_a.tevarindol.bookstore.book;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {
  private final BookService books;

  @GetMapping("/")
  public String home() {
    return "redirect:/books";
  }

  @GetMapping
  public String list(@RequestParam(defaultValue = "") String q, Model model) {
    model.addAttribute("books", books.availableBooks(q));
    model.addAttribute("query", q);
    return "books/list";
  }

  @GetMapping("/{id}")
  public String detail(@PathVariable long id, Model model) {
    model.addAttribute("book", books.get(id));
    return "books/detail";
  }
}
