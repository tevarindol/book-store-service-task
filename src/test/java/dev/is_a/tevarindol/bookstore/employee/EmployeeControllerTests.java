package dev.is_a.tevarindol.bookstore.employee;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import dev.is_a.tevarindol.bookstore.account.Account;
import dev.is_a.tevarindol.bookstore.account.AccountForm;
import dev.is_a.tevarindol.bookstore.book.Book;
import dev.is_a.tevarindol.bookstore.book.BookForm;
import dev.is_a.tevarindol.bookstore.book.BookService;
import dev.is_a.tevarindol.bookstore.client.ClientService;
import dev.is_a.tevarindol.bookstore.common.ConflictException;
import dev.is_a.tevarindol.bookstore.order.OrderService;
import dev.is_a.tevarindol.bookstore.order.StoreOrder;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Validator;

@SpringBootTest
class EmployeeControllerTests {
  private static final String EMAIL = "employee@example.com";
  private static final long ID = 42L;

  private final BookService books = mock(BookService.class);
  private final OrderService orders = mock(OrderService.class);
  private final ClientService clients = mock(ClientService.class);
  private final EmployeeService employees = mock(EmployeeService.class);
  private final Authentication authentication =
      UsernamePasswordAuthenticationToken.authenticated(EMAIL, null, List.of());

  private MockMvc mvc;

  @BeforeEach
  void setUp(
      @Autowired @Qualifier("mvcValidator") Validator validator, @Autowired ModelMapper mapper) {
    mvc =
        MockMvcBuilders.standaloneSetup(
                new EmployeeController(books, orders, clients, employees, mapper))
            .setValidator(validator)
            .build();
  }

  @Test
  void showsDashboard() throws Exception {
    mvc.perform(get("/employee")).andExpect(view().name("employee/dashboard"));
  }

  @Test
  void listsBooks() throws Exception {
    var catalog = List.of(mock(Book.class));
    when(books.allBooks()).thenReturn(catalog);

    mvc.perform(get("/employee/books"))
        .andExpect(view().name("employee/books"))
        .andExpect(model().attribute("books", catalog));
  }

  @Test
  void showsNewBookForm() throws Exception {
    mvc.perform(get("/employee/books/new"))
        .andExpect(view().name("employee/book-form"))
        .andExpect(model().attributeExists("bookForm"))
        .andExpect(model().attribute("editing", false));
  }

  @Test
  void showsEditFormWithStoredBook() throws Exception {
    BookForm form = new BookForm();
    form.setIsbn("ISBN-A");
    form.setTitle("Book A");
    form.setAuthor("Author A");
    form.setDescription("Description A");
    form.setPrice(new BigDecimal("10.25"));
    form.setActive(false);
    when(books.getIncludingInactive(ID)).thenReturn(new Book(form));

    mvc.perform(get("/employee/books/{id}/edit", ID))
        .andExpect(view().name("employee/book-form"))
        .andExpect(model().attribute("editing", true))
        .andExpect(model().attribute("bookId", ID))
        .andExpect(model().attribute("bookForm", hasProperty("isbn", is("ISBN-A"))))
        .andExpect(model().attribute("bookForm", hasProperty("active", is(false))));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void savesValidBookForm(boolean editing) throws Exception {
    mvc.perform(validBookForm(bookFormPost(editing))).andExpect(redirectedUrl("/employee/books"));

    if (editing) verify(books).update(eq(ID), any(BookForm.class));
    else verify(books).create(any(BookForm.class));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void rejectsInvalidBookFormBeforeSaving(boolean editing) throws Exception {
    var result =
        mvc.perform(bookFormPost(editing))
            .andExpect(view().name("employee/book-form"))
            .andExpect(model().attribute("editing", editing))
            .andExpect(model().attributeHasFieldErrors("bookForm", "isbn", "title", "price"));
    if (editing) result.andExpect(model().attribute("bookId", ID));

    verifyNoInteractions(books);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void duplicateIsbnStaysOnBookForm(boolean editing) throws Exception {
    ConflictException duplicate = new ConflictException("error.duplicateIsbn");
    if (editing) when(books.update(eq(ID), any(BookForm.class))).thenThrow(duplicate);
    else when(books.create(any(BookForm.class))).thenThrow(duplicate);

    var result =
        mvc.perform(validBookForm(bookFormPost(editing)))
            .andExpect(view().name("employee/book-form"))
            .andExpect(model().attribute("editing", editing))
            .andExpect(
                model().attributeHasFieldErrorCode("bookForm", "isbn", "error.duplicateIsbn"));
    if (editing) result.andExpect(model().attribute("bookId", ID));
  }

  @Test
  void deactivatesBookById() throws Exception {
    mvc.perform(post("/employee/books/{id}/delete", ID))
        .andExpect(redirectedUrl("/employee/books"));

    verify(books).deactivate(ID);
  }

  @Test
  void listsOrders() throws Exception {
    var history = List.of(mock(StoreOrder.class));
    when(orders.all()).thenReturn(history);

    mvc.perform(get("/employee/orders"))
        .andExpect(view().name("employee/orders"))
        .andExpect(model().attribute("orders", history));
  }

  @Test
  void confirmsOrderAsCurrentEmployee() throws Exception {
    mvc.perform(post("/employee/orders/{id}/confirm", ID).principal(authentication))
        .andExpect(redirectedUrl("/employee/orders"))
        .andExpect(flash().attribute("notice", "order.confirmed"));

    verify(orders).confirm(ID, EMAIL);
  }

  @Test
  void listsCustomers() throws Exception {
    var customers = List.of(mock(Account.class));
    when(clients.getAllClients()).thenReturn(customers);

    mvc.perform(get("/employee/customers"))
        .andExpect(view().name("employee/customers"))
        .andExpect(model().attribute("customers", customers));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void togglesCustomerBlockedState(boolean blocked) throws Exception {
    Account otherCustomer = mock(Account.class);
    when(otherCustomer.getId()).thenReturn(ID + 1);
    Account customer = mock(Account.class);
    when(customer.getId()).thenReturn(ID);
    when(customer.isBlocked()).thenReturn(blocked);
    when(clients.getAllClients()).thenReturn(List.of(otherCustomer, customer));

    mvc.perform(post("/employee/customers/{id}/block", ID))
        .andExpect(redirectedUrl("/employee/customers"));

    verify(clients).blockClient(ID, !blocked);
    verify(clients, never()).blockClient(eq(ID + 1), anyBoolean());
  }

  @Test
  void listsEmployeesAndShowsRegistrationForm() throws Exception {
    var staff = List.of(mock(Account.class));
    when(employees.getAllEmployees()).thenReturn(staff);

    mvc.perform(get("/employee/employees"))
        .andExpect(view().name("employee/employees"))
        .andExpect(model().attribute("employees", staff))
        .andExpect(model().attributeExists("accountForm"));
  }

  @Test
  void registersEmployeeWithValidForm() throws Exception {
    mvc.perform(employeeForm("vivid lunar orchard velvet compass 47"))
        .andExpect(redirectedUrl("/employee/employees"));

    verify(employees).addEmployee(any(AccountForm.class));
  }

  @Test
  void rejectsWeakEmployeePasswordBeforeRegistering() throws Exception {
    var staff = List.of(mock(Account.class));
    when(employees.getAllEmployees()).thenReturn(staff);

    mvc.perform(employeeForm("Password123"))
        .andExpect(view().name("employee/employees"))
        .andExpect(model().attribute("employees", staff))
        .andExpect(
            model().attributeHasFieldErrorCode("accountForm", "password", "PasswordStrength"));

    verify(employees).getAllEmployees();
    verifyNoMoreInteractions(employees);
  }

  @Test
  void duplicateEmailStaysOnEmployeeForm() throws Exception {
    var staff = List.of(mock(Account.class));
    when(employees.getAllEmployees()).thenReturn(staff);
    when(employees.addEmployee(any(AccountForm.class)))
        .thenThrow(new ConflictException("error.duplicateAccount"));

    mvc.perform(employeeForm("vivid lunar orchard velvet compass 47"))
        .andExpect(view().name("employee/employees"))
        .andExpect(model().attribute("employees", staff))
        .andExpect(
            model().attributeHasFieldErrorCode("accountForm", "email", "error.duplicateAccount"));
  }

  @Test
  void deletesAnotherEmployee() throws Exception {
    String otherEmail = "other@example.com";

    mvc.perform(post("/employee/employees/{email}/delete", otherEmail).principal(authentication))
        .andExpect(redirectedUrl("/employee/employees"));

    verify(employees).deleteEmployeeByEmail(otherEmail);
  }

  @Test
  void preventsSelfDeletionIgnoringEmailCase() throws Exception {
    mvc.perform(
            post("/employee/employees/{email}/delete", "EMPLOYEE@example.com")
                .principal(authentication))
        .andExpect(redirectedUrl("/employee/employees"));

    verifyNoInteractions(employees);
  }

  private static MockHttpServletRequestBuilder bookFormPost(boolean editing) {
    return editing ? post("/employee/books/{id}", ID) : post("/employee/books");
  }

  private static MockHttpServletRequestBuilder validBookForm(
      MockHttpServletRequestBuilder request) {
    return request
        .param("isbn", "ISBN-A")
        .param("title", "Book A")
        .param("author", "Author A")
        .param("description", "Description A")
        .param("price", "10.25");
  }

  private static MockHttpServletRequestBuilder employeeForm(String password) {
    return post("/employee/employees")
        .param("email", EMAIL)
        .param("firstName", "Ada")
        .param("lastName", "Reed")
        .param("password", password);
  }
}
