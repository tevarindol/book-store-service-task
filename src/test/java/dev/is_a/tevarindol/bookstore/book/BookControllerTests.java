package dev.is_a.tevarindol.bookstore.book;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import dev.is_a.tevarindol.bookstore.common.GlobalExceptionHandler;
import dev.is_a.tevarindol.bookstore.common.NotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BookControllerTests {
  private final BookService books = mock(BookService.class);
  private final Book book = new Book();
  private final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(new BookController(books))
          .setControllerAdvice(new GlobalExceptionHandler(new StaticMessageSource()))
          .build();

  @Test
  void redirectsTrailingSlashToCatalog() throws Exception {
    mvc.perform(get("/books/")).andExpect(redirectedUrl("/books"));

    verifyNoInteractions(books);
  }

  @Test
  void listsBooksWithEmptyQueryByDefault() throws Exception {
    when(books.availableBooks("")).thenReturn(List.of(book));

    mvc.perform(get("/books"))
        .andExpect(view().name("books/list"))
        .andExpect(model().attribute("books", List.of(book)))
        .andExpect(model().attribute("query", ""));
  }

  @Test
  void listsSearchResultsAndKeepsSubmittedQuery() throws Exception {
    when(books.availableBooks("Book A")).thenReturn(List.of(book));

    mvc.perform(get("/books").param("q", "Book A"))
        .andExpect(view().name("books/list"))
        .andExpect(model().attribute("books", List.of(book)))
        .andExpect(model().attribute("query", "Book A"));
  }

  @Test
  void showsBookDetailsById() throws Exception {
    when(books.get(42L)).thenReturn(book);

    mvc.perform(get("/books/{id}", 42L))
        .andExpect(view().name("books/detail"))
        .andExpect(model().attribute("book", book));
  }

  @Test
  void showsNotFoundPageForMissingBook() throws Exception {
    when(books.get(42L)).thenThrow(new NotFoundException("error.bookNotFound"));

    mvc.perform(get("/books/{id}", 42L))
        .andExpect(status().isNotFound())
        .andExpect(view().name("error/error"))
        .andExpect(model().attribute("status", 404))
        .andExpect(model().attribute("message", "error.bookNotFound"));
  }
}
