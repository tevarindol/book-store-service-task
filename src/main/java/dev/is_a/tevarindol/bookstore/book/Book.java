package dev.is_a.tevarindol.bookstore.book;

import dev.is_a.tevarindol.bookstore.common.BadRequestException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Entity
@Table(name = "books")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
public class Book {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 20)
  private String isbn;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String author;

  @Column(nullable = false, columnDefinition = "text")
  private String description;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal price;

  @Column(name = "stock_quantity", nullable = false)
  private int stockQuantity;

  @Column(nullable = false)
  private boolean active = true;

  public Book(BookForm form) {
    update(form);
  }

  public void update(BookForm form) {
    isbn = form.getIsbn().trim();
    title = form.getTitle().trim();
    author = form.getAuthor().trim();
    description = form.getDescription().trim();
    price = form.getPrice();
    stockQuantity = form.getStockQuantity();
    active = form.isActive();
  }

  public void takeFromStock(int quantity) {
    if (quantity < 1 || stockQuantity < quantity) {
      log.debug(
          "Stock deduction rejected: bookId={}, requested={}, available={}",
          id,
          quantity,
          stockQuantity);
      throw new BadRequestException("error.insufficientStock");
    }
    stockQuantity -= quantity;
  }

  public void updateActive(boolean active) {
    this.active = active;
  }
}
