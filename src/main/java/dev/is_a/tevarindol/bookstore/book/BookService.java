package dev.is_a.tevarindol.bookstore.book;

import dev.is_a.tevarindol.bookstore.common.ConflictException;
import dev.is_a.tevarindol.bookstore.common.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class BookService {
  private final BookRepository books;

  @Transactional(readOnly = true)
  public List<Book> availableBooks(String query) {
    return books.searchActive(query);
  }

  @Transactional(readOnly = true)
  public List<Book> allBooks() {
    return books.findAll();
  }

  @Transactional(readOnly = true)
  public Book get(long id) {
    return books
        .findByIdAndActiveTrue(id)
        .orElseThrow(() -> new NotFoundException("error.bookNotFound"));
  }

  @Transactional(readOnly = true)
  public Book getBookByName(String name) {
    return books
        .findByTitleIgnoreCaseAndActiveTrue(name)
        .orElseThrow(() -> new NotFoundException("error.bookNotFound"));
  }

  @Transactional(readOnly = true)
  public Book getIncludingInactive(long id) {
    return books.findById(id).orElseThrow(() -> new NotFoundException("error.bookNotFound"));
  }

  public Book create(BookForm form) {
    if (books.existsByIsbnIgnoreCase(form.getIsbn()))
      throw new ConflictException("error.duplicateIsbn");
    return books.save(new Book(form));
  }

  public Book addBook(BookForm form) {
    return create(form);
  }

  public Book update(long id, BookForm form) {
    Book book = getIncludingInactive(id);
    if (books.existsByIsbnIgnoreCaseAndIdNot(form.getIsbn(), id))
      throw new ConflictException("error.duplicateIsbn");
    book.update(form);
    return book;
  }

  public Book updateBookByName(String name, BookForm form) {
    Book book = getBookByName(name);
    return update(book.getId(), form);
  }

  public void deactivate(long id) {
    getIncludingInactive(id).updateActive(false);
  }

  public void deleteBookByName(String name) {
    deactivate(getBookByName(name).getId());
  }
}
