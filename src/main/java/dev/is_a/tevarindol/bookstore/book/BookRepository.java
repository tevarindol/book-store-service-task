package dev.is_a.tevarindol.bookstore.book;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {
  @Query(
      "SELECT b FROM Book b "
          + "WHERE b.active = true "
          + "AND (:query IS NULL OR TRIM(:query) = '' "
          + "OR LOWER(b.title) LIKE LOWER(CONCAT('%', TRIM(:query), '%')) "
          + "OR LOWER(b.author) LIKE LOWER(CONCAT('%', TRIM(:query), '%'))) "
          + "ORDER BY b.title ASC")
  List<Book> searchActive(@Param("query") String query);

  Optional<Book> findByIdAndActiveTrue(Long id);

  Optional<Book> findByIsbnIgnoreCase(String isbn);

  Optional<Book> findByTitleIgnoreCaseAndActiveTrue(String title);

  boolean existsByIsbnIgnoreCase(String isbn);

  boolean existsByIsbnIgnoreCaseAndIdNot(String isbn, Long id);
}
