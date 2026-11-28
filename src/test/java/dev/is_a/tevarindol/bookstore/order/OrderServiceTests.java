package dev.is_a.tevarindol.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.is_a.tevarindol.bookstore.account.Account;
import dev.is_a.tevarindol.bookstore.account.AccountRepository;
import dev.is_a.tevarindol.bookstore.account.AccountRole;
import dev.is_a.tevarindol.bookstore.book.Book;
import dev.is_a.tevarindol.bookstore.book.BookForm;
import dev.is_a.tevarindol.bookstore.book.BookRepository;
import dev.is_a.tevarindol.bookstore.common.BadRequestException;
import dev.is_a.tevarindol.bookstore.common.NotFoundException;
import dev.is_a.tevarindol.bookstore.order.OrderDTO.OrderLineDTO;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class OrderServiceTests {
  private static final String CUSTOMER_EMAIL = "order-reader@example.com";
  private static final String EMPLOYEE_EMAIL = "order-employee@example.com";

  @Autowired private OrderService service;
  @Autowired private OrderRepository orders;
  @Autowired private AccountRepository accounts;
  @Autowired private BookRepository books;
  @Autowired private JdbcTemplate jdbc;

  private final List<Long> accountIds = new ArrayList<>();
  private final List<Long> bookIds = new ArrayList<>();

  @AfterEach
  void removeTestData() {
    accounts.deleteAllById(accountIds);
    books.deleteAllById(bookIds);
  }

  @Test
  void addOrderMergesRepeatedBooksAndPersistsOrderAndStock() {
    createAccount(CUSTOMER_EMAIL, AccountRole.CUSTOMER);
    Book first = createBook("ORDER-1", "10.25", 5, true);
    Book second = createBook("ORDER-2", "3.40", 2, true);
    OrderDTO request =
        new OrderDTO(
            List.of(
                new OrderLineDTO(first.getId(), 1),
                new OrderLineDTO(second.getId(), 1),
                new OrderLineDTO(first.getId(), 2)));

    StoreOrder submitted = service.addOrder("ORDER-READER@example.com", request);

    assertThat(service.forCustomer(CUSTOMER_EMAIL))
        .singleElement()
        .satisfies(
            stored -> {
              assertThat(stored.getId()).isEqualTo(submitted.getId());
              assertThat(stored.getStatus()).isEqualTo(OrderStatus.SUBMITTED);
              assertThat(stored.getItems())
                  .extracting(OrderItem::getQuantity)
                  .containsExactly(3, 1);
              assertThat(stored.getTotal()).isEqualByComparingTo("34.15");
            });
    assertThat(stockOf(first)).isEqualTo(2);
    assertThat(stockOf(second)).isEqualTo(1);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void addOrderRejectsMissingOrEmptyLines(List<OrderLineDTO> lines) {
    assertThatThrownBy(() -> service.addOrder(CUSTOMER_EMAIL, new OrderDTO(lines)))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("error.emptyOrder");
  }

  @Test
  void addOrderRejectsMissingBookIdAndNonPositiveQuantity() {
    assertThatThrownBy(
            () ->
                service.addOrder(CUSTOMER_EMAIL, new OrderDTO(List.of(new OrderLineDTO(null, 1)))))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("error.orderLineBook");
    assertThatThrownBy(
            () -> service.addOrder(CUSTOMER_EMAIL, new OrderDTO(List.of(new OrderLineDTO(1L, 0)))))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("error.positiveQuantity");
  }

  @Test
  void submitRejectsEmptyBasketAndMissingCustomer() {
    assertThatThrownBy(() -> service.submit(CUSTOMER_EMAIL, Map.of()))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("error.emptyBasket");
    assertThatThrownBy(() -> service.submit(CUSTOMER_EMAIL, Map.of(1L, 1)))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("error.customerNotFound");
  }

  @Test
  void submitRejectsInactiveBook() {
    createAccount(CUSTOMER_EMAIL, AccountRole.CUSTOMER);
    Book inactive = createBook("ORDER-INACTIVE", "10.25", 5, false);

    assertThatThrownBy(() -> service.submit(CUSTOMER_EMAIL, Map.of(inactive.getId(), 1)))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("error.unavailableBook");
    assertThat(stockOf(inactive)).isEqualTo(5);
  }

  @Test
  void submitRollsBackStockWhenLaterBookHasInsufficientStock() {
    createAccount(CUSTOMER_EMAIL, AccountRole.CUSTOMER);
    Book available = createBook("ORDER-FIRST", "10.25", 5, true);
    Book scarce = createBook("ORDER-SECOND", "3.40", 1, true);
    var requested = new LinkedHashMap<Long, Integer>();
    requested.put(available.getId(), 2);
    requested.put(scarce.getId(), 2);

    assertThatThrownBy(() -> service.submit(CUSTOMER_EMAIL, requested))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("error.insufficientStock");

    assertThat(stockOf(available)).isEqualTo(5);
    assertThat(stockOf(scarce)).isEqualTo(1);
    assertThat(service.forCustomer(CUSTOMER_EMAIL)).isEmpty();
  }

  @Test
  void confirmPersistsEmployeeAndRejectsRepeatedConfirmation() {
    createAccount(CUSTOMER_EMAIL, AccountRole.CUSTOMER);
    createAccount(EMPLOYEE_EMAIL, AccountRole.EMPLOYEE);
    Book book = createBook("ORDER-CONFIRM", "10.25", 5, true);
    long id = service.submit(CUSTOMER_EMAIL, Map.of(book.getId(), 1)).getId();

    assertThat(service.confirm(id, EMPLOYEE_EMAIL).getId()).isEqualTo(id);

    StoreOrder confirmed = service.forCustomer(CUSTOMER_EMAIL).get(0);
    assertThat(confirmed.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    assertThat(confirmed.getConfirmedAt()).isNotNull();
    assertThat(confirmed.getEmployee().getEmail()).isEqualTo(EMPLOYEE_EMAIL);
    assertThatThrownBy(() -> service.confirm(id, EMPLOYEE_EMAIL))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("error.confirmSubmittedOrder");
  }

  @Test
  void confirmRejectsMissingOrderOrEmployee() {
    createAccount(CUSTOMER_EMAIL, AccountRole.CUSTOMER);
    Book book = createBook("ORDER-UNCONFIRMED", "10.25", 5, true);
    long id = service.submit(CUSTOMER_EMAIL, Map.of(book.getId(), 1)).getId();

    assertThatThrownBy(() -> service.confirm(-1L, EMPLOYEE_EMAIL))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("error.orderNotFound");
    assertThatThrownBy(() -> service.confirm(id, EMPLOYEE_EMAIL))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("error.employeeNotFound");
    assertThat(orders.findById(id).orElseThrow().getStatus()).isEqualTo(OrderStatus.SUBMITTED);
  }

  @Test
  void orderListsLoadDetailsNewestFirstAndFilterByCustomer() {
    String otherEmail = "other-order-reader@example.com";
    createAccount(CUSTOMER_EMAIL, AccountRole.CUSTOMER);
    createAccount(otherEmail, AccountRole.CUSTOMER);
    Book book = createBook("ORDER-HISTORY", "10.25", 5, true);
    Map<Long, Integer> requested = Map.of(book.getId(), 1);
    StoreOrder newest = service.submit(CUSTOMER_EMAIL, requested);
    StoreOrder other = service.submit(otherEmail, requested);
    StoreOrder oldest = service.submit(CUSTOMER_EMAIL, requested);
    LocalDateTime firstDay = LocalDateTime.of(2026, 1, 1, 0, 0);
    jdbc.update(
        "UPDATE orders SET created_at = ? WHERE id = ?",
        Timestamp.valueOf(firstDay),
        oldest.getId());
    jdbc.update(
        "UPDATE orders SET created_at = ? WHERE id = ?",
        Timestamp.valueOf(firstDay.plusDays(1)),
        other.getId());
    jdbc.update(
        "UPDATE orders SET created_at = ? WHERE id = ?",
        Timestamp.valueOf(firstDay.plusDays(2)),
        newest.getId());

    List<StoreOrder> history = service.forCustomer("ORDER-READER@example.com");
    assertThat(history)
        .extracting(StoreOrder::getId)
        .containsExactly(newest.getId(), oldest.getId());
    assertThat(history.get(0).getCustomer().getEmail()).isEqualTo(CUSTOMER_EMAIL);
    assertThat(history.get(0).getItems())
        .extracting(OrderItem::getBookTitle)
        .containsExactly("Test book");
    assertThat(service.all())
        .extracting(StoreOrder::getId)
        .containsSubsequence(newest.getId(), other.getId(), oldest.getId());
  }

  private void createAccount(String email, AccountRole role) {
    Account account = accounts.save(new Account(email, "unused-hash", "Ada", "Reed", null, role));
    accountIds.add(account.getId());
  }

  private Book createBook(String isbn, String price, int stock, boolean active) {
    BookForm form = new BookForm();
    form.setIsbn(isbn);
    form.setTitle("Test book");
    form.setAuthor("Test author");
    form.setDescription("Test description");
    form.setPrice(new BigDecimal(price));
    form.setStockQuantity(stock);
    form.setActive(active);
    Book book = books.save(new Book(form));
    bookIds.add(book.getId());
    return book;
  }

  private int stockOf(Book book) {
    return books.findById(book.getId()).orElseThrow().getStockQuantity();
  }
}
