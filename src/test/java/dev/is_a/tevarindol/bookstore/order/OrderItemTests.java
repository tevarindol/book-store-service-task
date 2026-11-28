package dev.is_a.tevarindol.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import dev.is_a.tevarindol.bookstore.book.Book;
import dev.is_a.tevarindol.bookstore.book.BookForm;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrderItemTests {
  @Test
  void keepsOriginalTitleAndPriceWhenBookChanges() {
    BookForm form = bookForm();
    Book book = new Book(form);
    OrderItem item = new OrderItem(book, 3);

    form.setTitle("Updated title");
    form.setPrice(new BigDecimal("12.50"));
    book.update(form);

    assertThat(item.getQuantity()).isEqualTo(3);
    assertThat(item.getBookTitle()).isEqualTo("Original title");
    assertThat(item.getSubtotal()).isEqualByComparingTo("30.75");
  }

  @Test
  void subtotalMultipliesUnitPriceByQuantity() {
    OrderItem item = new OrderItem(new Book(bookForm()), 3);

    assertThat(item.getSubtotal()).isEqualByComparingTo("30.75");
  }

  private static BookForm bookForm() {
    BookForm form = new BookForm();
    form.setIsbn("ISBN-A");
    form.setTitle("Original title");
    form.setAuthor("Author A");
    form.setDescription("Description A");
    form.setPrice(new BigDecimal("10.25"));
    return form;
  }
}
