package dev.is_a.tevarindol.bookstore.employee;

import dev.is_a.tevarindol.bookstore.account.AccountForm;
import dev.is_a.tevarindol.bookstore.book.BookForm;
import dev.is_a.tevarindol.bookstore.book.BookService;
import dev.is_a.tevarindol.bookstore.client.ClientService;
import dev.is_a.tevarindol.bookstore.common.ConflictException;
import dev.is_a.tevarindol.bookstore.common.NotFoundException;
import dev.is_a.tevarindol.bookstore.order.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/employee")
@PreAuthorize("hasRole('EMPLOYEE')")
@RequiredArgsConstructor
public class EmployeeController {
  private final BookService books;
  private final OrderService orders;
  private final ClientService clients;
  private final EmployeeService employees;
  private final ModelMapper mapper;

  @GetMapping
  public String dashboard() {
    return "employee/dashboard";
  }

  @GetMapping("/books")
  public String bookList(Model model) {
    model.addAttribute("books", books.allBooks());
    return "employee/books";
  }

  @GetMapping("/books/new")
  public String newBook(Model model) {
    model.addAttribute("bookForm", new BookForm());
    model.addAttribute("editing", false);
    return "employee/book-form";
  }

  @PostMapping("/books")
  public String createBook(
      @Valid @ModelAttribute BookForm bookForm, BindingResult result, Model model) {
    if (result.hasErrors()) {
      model.addAttribute("editing", false);
      return "employee/book-form";
    }
    try {
      books.create(bookForm);
    } catch (ConflictException e) {
      result.rejectValue("isbn", e.getMessage());
      model.addAttribute("editing", false);
      return "employee/book-form";
    }
    return "redirect:/employee/books";
  }

  @GetMapping("/books/{id}/edit")
  public String editBook(@PathVariable long id, Model model) {
    var book = books.getIncludingInactive(id);
    BookForm form = mapper.map(book, BookForm.class);
    model.addAttribute("bookForm", form);
    model.addAttribute("editing", true);
    model.addAttribute("bookId", id);
    return "employee/book-form";
  }

  @PostMapping("/books/{id}")
  public String updateBook(
      @PathVariable long id,
      @Valid @ModelAttribute BookForm bookForm,
      BindingResult result,
      Model model) {
    if (result.hasErrors()) {
      model.addAttribute("editing", true);
      model.addAttribute("bookId", id);
      return "employee/book-form";
    }
    try {
      books.update(id, bookForm);
    } catch (ConflictException e) {
      result.rejectValue("isbn", e.getMessage());
      model.addAttribute("editing", true);
      model.addAttribute("bookId", id);
      return "employee/book-form";
    }
    return "redirect:/employee/books";
  }

  @PostMapping("/books/{id}/delete")
  public String deleteBook(@PathVariable long id) {
    books.deactivate(id);
    return "redirect:/employee/books";
  }

  @GetMapping("/orders")
  public String orderList(Model model) {
    model.addAttribute("orders", orders.all());
    return "employee/orders";
  }

  @PostMapping("/orders/{id}/confirm")
  public String confirmOrder(
      @PathVariable long id, Authentication authentication, RedirectAttributes redirect) {
    orders.confirm(id, authentication.getName());
    redirect.addFlashAttribute("notice", "order.confirmed");
    return "redirect:/employee/orders";
  }

  @GetMapping("/customers")
  public String customerList(Model model) {
    model.addAttribute("customers", clients.getAllClients());
    return "employee/customers";
  }

  @PostMapping("/customers/{id}/block")
  public String toggleBlock(@PathVariable long id) {
    var account =
        clients.getAllClients().stream()
            .filter(c -> c.getId() == id)
            .findFirst()
            .orElseThrow(() -> new NotFoundException("error.customerNotFound"));
    clients.blockClient(id, !account.isBlocked());
    return "redirect:/employee/customers";
  }

  @GetMapping("/employees")
  public String employeeList(Model model) {
    model.addAttribute("employees", employees.getAllEmployees());
    model.addAttribute("accountForm", new AccountForm());
    return "employee/employees";
  }

  @PostMapping("/employees")
  public String addEmployee(
      @Valid @ModelAttribute AccountForm accountForm, BindingResult result, Model model) {
    if (result.hasErrors()) {
      model.addAttribute("employees", employees.getAllEmployees());
      return "employee/employees";
    }
    try {
      employees.addEmployee(accountForm);
    } catch (ConflictException e) {
      result.rejectValue("email", e.getMessage());
      model.addAttribute("employees", employees.getAllEmployees());
      return "employee/employees";
    }
    return "redirect:/employee/employees";
  }

  @PostMapping("/employees/{email}/delete")
  public String deleteEmployee(@PathVariable String email, Authentication authentication) {
    if (!email.equalsIgnoreCase(authentication.getName())) employees.deleteEmployeeByEmail(email);
    return "redirect:/employee/employees";
  }
}
