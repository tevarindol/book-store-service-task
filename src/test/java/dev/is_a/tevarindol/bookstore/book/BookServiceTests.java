package dev.is_a.tevarindol.bookstore.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.is_a.tevarindol.bookstore.common.ConflictException;
import dev.is_a.tevarindol.bookstore.common.NotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BookServiceTests {
  @Autowired private BookService service;
  @Autowired private BookRepository books;

  private final List<Long> createdBookIds = new ArrayList<>();

  @AfterEach
  void removeTestBooks() {
    books.deleteAllById(createdBookIds);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = "   ")
  void unfilteredCatalogIncludesActiveBooksInTitleOrder(String query) {
    Book later = create(form("CAT-2", "Book B", true));
    Book earlier = create(form("CAT-1", "Book A", true));
    Book hidden = create(form("CAT-3", "Book C", false));

    assertThat(service.availableBooks(query))
        .extracting(Book::getId)
        .containsSubsequence(earlier.getId(), later.getId())
        .doesNotContain(hidden.getId());
    assertThat(service.allBooks())
        .extracting(Book::getId)
        .contains(earlier.getId(), later.getId(), hidden.getId());
  }

  @Test
  void catalogMatchesTitleOrAuthorIgnoringCaseAndOuterSpaces() {
    Book byTitle = create(form("SEARCH-1", "Book A", true));
    BookForm authorMatch = form("SEARCH-2", "Book B", true);
    authorMatch.setAuthor("Author Book A");
    Book byAuthor = create(authorMatch);
    create(form("SEARCH-3", "Book AA", false));
    create(form("SEARCH-4", "Book C", true));

    assertThat(service.availableBooks("  oOk a  "))
        .extracting(Book::getId)
        .containsExactly(byTitle.getId(), byAuthor.getId());
    assertThat(service.availableBooks("no-catalog-match")).isEmpty();
  }

  @Test
  void publicLookupsFindActiveBooksAndRejectMissingOrInactiveBooks() {
    Book active = create(form("FIND-1", "Book A", true));
    Book inactive = create(form("FIND-2", "Book B", false));

    assertThat(service.get(active.getId()).getId()).isEqualTo(active.getId());
    assertThat(service.getIncludingInactive(inactive.getId()).isActive()).isFalse();

    assertNotFound(() -> service.get(inactive.getId()));
    assertNotFound(() -> service.get(-1L));
    assertNotFound(() -> service.getIncludingInactive(-1L));
  }

  @Test
  void createPersistsFormAndRejectsDuplicateIsbnIgnoringCase() {
    BookForm form = form("ISBN-ADD", "  Book A  ", true);
    Book added = create(form);

    Book stored = service.get(added.getId());
    assertThat(stored.getIsbn()).isEqualTo("ISBN-ADD");
    assertThat(stored.getTitle()).isEqualTo("Book A");
    assertThat(stored.getAuthor()).isEqualTo(form.getAuthor());
    assertThat(stored.getDescription()).isEqualTo(form.getDescription());
    assertThat(stored.getPrice()).isEqualByComparingTo(form.getPrice());
    assertThat(stored.getStockQuantity()).isEqualTo(form.getStockQuantity());
    assertThatThrownBy(() -> service.create(form("isbn-add", "Book B", true)))
        .isInstanceOf(ConflictException.class)
        .hasMessage("error.duplicateIsbn");
  }

  @Test
  void updatesInactiveBookWithItsOwnIsbnAndRejectsAnotherBooksIsbn() {
    Book target = create(form("UPDATE-1", "Book A", false));
    create(form("UPDATE-2", "Book B", true));
    BookForm updated = form("UPDATE-1", "Book C", false);
    updated.setPrice(new BigDecimal("24.50"));
    updated.setStockQuantity(9);

    assertThat(service.update(target.getId(), updated).getId()).isEqualTo(target.getId());
    Book stored = service.getIncludingInactive(target.getId());
    assertThat(stored.getTitle()).isEqualTo("Book C");
    assertThat(stored.getPrice()).isEqualByComparingTo("24.50");
    assertThat(stored.getStockQuantity()).isEqualTo(9);
    assertThat(stored.isActive()).isFalse();

    assertThatThrownBy(() -> service.update(target.getId(), form("update-2", "Book D", true)))
        .isInstanceOf(ConflictException.class)
        .hasMessage("error.duplicateIsbn");
    Book unchanged = service.getIncludingInactive(target.getId());
    assertThat(unchanged.getIsbn()).isEqualTo("UPDATE-1");
    assertThat(unchanged.getTitle()).isEqualTo("Book C");
    assertThat(unchanged.isActive()).isFalse();
  }

  @Test
  void updatePersistsNewIsbnAndReactivatesBook() {
    Book inactive = create(form("CHANGE-1", "Book A", false));

    service.update(inactive.getId(), form("CHANGE-2", "Book A", true));

    Book stored = service.get(inactive.getId());
    assertThat(stored.getIsbn()).isEqualTo("CHANGE-2");
    assertThat(stored.isActive()).isTrue();
  }

  @Test
  void updateAndDeactivateRejectMissingId() {
    assertNotFound(() -> service.update(-1L, form("UPDATE-1", "Book A", true)));
    assertNotFound(() -> service.deactivate(-1L));
  }

  @Test
  void deactivationByIdHidesBookWithoutDeletingIt() {
    Book byId = create(form("REMOVE-1", "Book A", true));

    service.deactivate(byId.getId());
    service.deactivate(byId.getId());

    assertThat(service.getIncludingInactive(byId.getId()).isActive()).isFalse();
    assertThat(service.availableBooks("Book")).extracting(Book::getId).doesNotContain(byId.getId());
  }

  private Book create(BookForm form) {
    Book book = service.create(form);
    createdBookIds.add(book.getId());
    return book;
  }

  private static void assertNotFound(Runnable lookup) {
    assertThatThrownBy(lookup::run)
        .isInstanceOf(NotFoundException.class)
        .hasMessage("error.bookNotFound");
  }

  private static BookForm form(String isbn, String title, boolean active) {
    BookForm form = new BookForm();
    form.setIsbn(isbn);
    form.setTitle(title);
    form.setAuthor("Test Author");
    form.setDescription("Test description");
    form.setPrice(new BigDecimal("12.50"));
    form.setStockQuantity(3);
    form.setActive(active);
    return form;
  }
}
