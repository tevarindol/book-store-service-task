package dev.is_a.tevarindol.bookstore.order;

import dev.is_a.tevarindol.bookstore.account.Account;
import dev.is_a.tevarindol.bookstore.common.BadRequestException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreOrder {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private Account customer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employee_id")
  private Account employee;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus status;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "confirmed_at")
  private LocalDateTime confirmedAt;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id ASC")
  private List<OrderItem> items = new ArrayList<>();

  public StoreOrder(Account customer, List<OrderItem> items) {
    this.customer = customer;
    this.status = OrderStatus.SUBMITTED;
    this.createdAt = LocalDateTime.now();
    items.forEach(
        item -> {
          item.setOrder(this);
          this.items.add(item);
        });
  }

  public void confirm(Account employee) {
    if (status != OrderStatus.SUBMITTED)
      throw new BadRequestException("error.confirmSubmittedOrder");
    this.status = OrderStatus.CONFIRMED;
    this.employee = employee;
    this.confirmedAt = LocalDateTime.now();
  }

  public BigDecimal getTotal() {
    return items.stream().map(OrderItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
