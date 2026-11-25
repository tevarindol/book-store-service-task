package dev.is_a.tevarindol.bookstore.order;

import dev.is_a.tevarindol.bookstore.book.Book;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  @Setter(AccessLevel.PACKAGE)
  private StoreOrder order;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "book_id", nullable = false)
  @Getter
  private Book book;

  @Column(name = "book_title", nullable = false)
  @Getter
  private String bookTitle;

  @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
  @Getter
  private BigDecimal unitPrice;

  @Column(nullable = false)
  @Getter
  private int quantity;

  public OrderItem(Book book, int quantity) {
    this.book = book;
    this.bookTitle = book.getTitle();
    this.unitPrice = book.getPrice();
    this.quantity = quantity;
  }

  public BigDecimal getSubtotal() {
    return unitPrice.multiply(BigDecimal.valueOf(quantity));
  }
}
