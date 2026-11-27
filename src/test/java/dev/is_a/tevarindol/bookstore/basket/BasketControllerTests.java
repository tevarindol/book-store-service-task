package dev.is_a.tevarindol.bookstore.basket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import dev.is_a.tevarindol.bookstore.basket.BasketController.BasketLine;
import dev.is_a.tevarindol.bookstore.book.Book;
import dev.is_a.tevarindol.bookstore.book.BookForm;
import dev.is_a.tevarindol.bookstore.book.BookService;
import dev.is_a.tevarindol.bookstore.common.BadRequestException;
import dev.is_a.tevarindol.bookstore.common.GlobalExceptionHandler;
import dev.is_a.tevarindol.bookstore.order.OrderDTO;
import dev.is_a.tevarindol.bookstore.order.OrderService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BasketControllerTests {
  private static final String EMAIL = "reader@example.com";

  private final Authentication authentication =
      UsernamePasswordAuthenticationToken.authenticated(EMAIL, null, List.of());
  private final MockHttpSession session = new MockHttpSession();
  private final BasketService basket = new BasketService();
  private final BookService books = mock(BookService.class);
  private final OrderService orders = mock(OrderService.class);
  private final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(new BasketController(basket, books, orders))
          .setControllerAdvice(new GlobalExceptionHandler(new StaticMessageSource()))
          .build();

  @BeforeEach
  void configureBasket() {
    ReflectionTestUtils.setField(basket, "sessionKey", "testBasket");
  }

  @Test
  void showsEmptyBasketWithZeroTotal() throws Exception {
    mvc.perform(get("/basket").session(session))
        .andExpect(view().name("basket/view"))
        .andExpect(model().attribute("lines", List.of()))
        .andExpect(model().attribute("total", BigDecimal.ZERO));

    verifyNoInteractions(books);
  }

  @Test
  void showsBasketLinesAndTotal() throws Exception {
    Book first = book("10.25", 5);
    Book second = book("3.40", 5);
    when(books.get(42L)).thenReturn(first);
    when(books.get(7L)).thenReturn(second);
    basket.add(session, 42L);
    basket.add(session, 42L);
    basket.add(session, 7L);

    var expectedLines = List.of(new BasketLine(first, 2), new BasketLine(second, 1));
    var result =
        mvc.perform(get("/basket").session(session))
            .andExpect(view().name("basket/view"))
            .andExpect(model().attribute("lines", expectedLines))
            .andExpect(model().attribute("total", new BigDecimal("23.90")))
            .andReturn();

    var lines = (List<?>) result.getModelAndView().getModel().get("lines");
    assertThat(lines)
        .extracting(line -> ((BasketLine) line).subtotal())
        .containsExactly(new BigDecimal("20.50"), new BigDecimal("3.40"));
  }

  @Test
  void addsLastAvailableCopy() throws Exception {
    when(books.get(42L)).thenReturn(book("10.25", 2));
    basket.add(session, 42L);

    mvc.perform(post("/basket/{id}", 42L).session(session))
        .andExpect(redirectedUrl("/basket"))
        .andExpect(flash().attribute("notice", "book.addedToBasket"));

    assertThat(basket.get(session)).containsExactly(entry(42L, 2));
  }

  @Test
  void rejectsAddingAboveAvailableStock() throws Exception {
    when(books.get(42L)).thenReturn(book("10.25", 1));
    basket.add(session, 42L);

    mvc.perform(post("/basket/{id}", 42L).session(session))
        .andExpect(redirectedUrl("/basket"))
        .andExpect(flash().attribute("error", "basket.insufficientStock"));

    assertThat(basket.get(session)).containsExactly(entry(42L, 1));
  }

  @Test
  void removesOnlySelectedBasketLine() throws Exception {
    basket.add(session, 42L);
    basket.add(session, 42L);
    basket.add(session, 7L);

    mvc.perform(post("/basket/{id}/remove", 42L).session(session))
        .andExpect(redirectedUrl("/basket"));

    assertThat(basket.get(session)).containsExactly(entry(7L, 1));
  }

  @Test
  void checkoutSubmitsAllLinesForCurrentCustomerAndClearsBasket() throws Exception {
    basket.add(session, 42L);
    basket.add(session, 42L);
    basket.add(session, 7L);

    mvc.perform(post("/checkout").session(session).principal(authentication))
        .andExpect(redirectedUrl("/orders"))
        .andExpect(flash().attribute("notice", "order.submitted"));

    var order = ArgumentCaptor.forClass(OrderDTO.class);
    verify(orders).addOrder(eq(EMAIL), order.capture());
    assertThat(order.getValue().getItems())
        .extracting(OrderDTO.OrderLineDTO::getBookId, OrderDTO.OrderLineDTO::getQuantity)
        .containsExactly(tuple(42L, 2), tuple(7L, 1));
    assertThat(basket.get(session)).isEmpty();
  }

  @Test
  void rejectedCheckoutKeepsBasketContents() throws Exception {
    basket.add(session, 42L);
    basket.add(session, 7L);
    when(orders.addOrder(eq(EMAIL), any(OrderDTO.class)))
        .thenThrow(new BadRequestException("error.insufficientStock"));

    mvc.perform(post("/checkout").session(session).principal(authentication))
        .andExpect(status().isBadRequest())
        .andExpect(view().name("error/error"))
        .andExpect(flash().attributeCount(0));

    assertThat(basket.get(session)).containsExactly(entry(42L, 1), entry(7L, 1));
  }

  private Book book(String price, int stock) {
    BookForm form = new BookForm();
    form.setIsbn("9780141439518");
    form.setTitle("Book");
    form.setAuthor("Author");
    form.setDescription("Description");
    form.setPrice(new BigDecimal(price));
    form.setStockQuantity(stock);
    return new Book(form);
  }
}
