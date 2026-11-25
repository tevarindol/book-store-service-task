package dev.is_a.tevarindol.bookstore.book;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
  List<Book> findByActiveTrueOrderByTitleAsc();

  Optional<Book> findByIdAndActiveTrue(Long id);

  Optional<Book> findByIsbnIgnoreCase(String isbn);

  Optional<Book> findByTitleIgnoreCaseAndActiveTrue(String title);

  boolean existsByIsbnIgnoreCase(String isbn);

  boolean existsByIsbnIgnoreCaseAndIdNot(String isbn, Long id);
}
