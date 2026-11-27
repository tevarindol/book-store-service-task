package dev.is_a.tevarindol.bookstore.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.is_a.tevarindol.bookstore.common.BadRequestException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BookTests {
  private final Book book = new Book(bookForm());

  @Test
  void constructorCopiesFormAndTrimsTextFields() {
    assertThat(book.getIsbn()).isEqualTo("ISBN-A");
    assertThat(book.getTitle()).isEqualTo("Book A");
    assertThat(book.getAuthor()).isEqualTo("Author A");
    assertThat(book.getDescription()).isEqualTo("Description A");
    assertThat(book.getPrice()).isEqualByComparingTo("10.50");
    assertThat(book.getStockQuantity()).isEqualTo(3);
    assertThat(book.isActive()).isTrue();
  }

  @Test
  void updateReplacesAllFormFieldsAndTrimsText() {
    BookForm form = new BookForm();
    form.setIsbn("  ISBN-B  ");
    form.setTitle("  Book B  ");
    form.setAuthor("  Author B  ");
    form.setDescription("  Description B  ");
    form.setPrice(new BigDecimal("15.25"));
    form.setStockQuantity(7);
    form.setActive(false);

    book.update(form);

    assertThat(book.getIsbn()).isEqualTo("ISBN-B");
    assertThat(book.getTitle()).isEqualTo("Book B");
    assertThat(book.getAuthor()).isEqualTo("Author B");
    assertThat(book.getDescription()).isEqualTo("Description B");
    assertThat(book.getPrice()).isEqualByComparingTo("15.25");
    assertThat(book.getStockQuantity()).isEqualTo(7);
    assertThat(book.isActive()).isFalse();
  }

  @Test
  void takeFromStockReducesQuantityAndAllowsTakingTheLastCopy() {
    book.takeFromStock(2);
    assertThat(book.getStockQuantity()).isEqualTo(1);

    book.takeFromStock(1);
    assertThat(book.getStockQuantity()).isZero();
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 0, 4})
  void takeFromStockRejectsInvalidQuantityWithoutChangingStock(int quantity) {
    assertThatThrownBy(() -> book.takeFromStock(quantity))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("error.insufficientStock");

    assertThat(book.getStockQuantity()).isEqualTo(3);
  }

  @Test
  void updateActiveChangesVisibilityWithoutChangingStock() {
    book.updateActive(false);
    assertThat(book.isActive()).isFalse();
    assertThat(book.getStockQuantity()).isEqualTo(3);

    book.updateActive(true);
    assertThat(book.isActive()).isTrue();
    assertThat(book.getStockQuantity()).isEqualTo(3);
  }

  private static BookForm bookForm() {
    BookForm form = new BookForm();
    form.setIsbn("  ISBN-A  ");
    form.setTitle("  Book A  ");
    form.setAuthor("  Author A  ");
    form.setDescription("  Description A  ");
    form.setPrice(new BigDecimal("10.50"));
    form.setStockQuantity(3);
    form.setActive(true);
    return form;
  }
}
