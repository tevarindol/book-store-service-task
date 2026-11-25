package dev.is_a.tevarindol.bookstore.order;

import dev.is_a.tevarindol.bookstore.account.Account;
import dev.is_a.tevarindol.bookstore.account.AccountRepository;
import dev.is_a.tevarindol.bookstore.book.Book;
import dev.is_a.tevarindol.bookstore.book.BookRepository;
import dev.is_a.tevarindol.bookstore.common.BadRequestException;
import dev.is_a.tevarindol.bookstore.common.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class OrderService {
  private final OrderRepository orders;
  private final AccountRepository accounts;
  private final BookRepository books;

  public StoreOrder submit(String customerEmail, Map<Long, Integer> requestedItems) {
    if (requestedItems.isEmpty()) throw new BadRequestException("error.emptyBasket");
    Account customer =
        accounts
            .findByEmailIgnoreCase(customerEmail)
            .orElseThrow(() -> new NotFoundException("error.customerNotFound"));
    List<OrderItem> items = new ArrayList<>();
    for (var entry : requestedItems.entrySet()) {
      Book book =
          books
              .findByIdAndActiveTrue(entry.getKey())
              .orElseThrow(() -> new NotFoundException("error.unavailableBook"));
      book.takeFromStock(entry.getValue());
      items.add(new OrderItem(book, entry.getValue()));
    }
    StoreOrder order = orders.save(new StoreOrder(customer, items));
    log.info("Order {} submitted by {}", order.getId(), customerEmail);
    return order;
  }

  public StoreOrder addOrder(String customerEmail, OrderDTO orderDto) {
    List<OrderDTO.OrderLineDTO> lines =
        Optional.ofNullable(orderDto.getItems())
            .filter(items -> !items.isEmpty())
            .orElseThrow(() -> new BadRequestException("error.emptyOrder"));
    Map<Long, Integer> requested = new java.util.LinkedHashMap<>();
    for (OrderDTO.OrderLineDTO line : lines) {
      Long bookId =
          Optional.ofNullable(line.getBookId())
              .orElseThrow(() -> new BadRequestException("error.orderLineBook"));
      if (line.getQuantity() < 1) throw new BadRequestException("error.positiveQuantity");
      requested.merge(bookId, line.getQuantity(), Integer::sum);
    }
    return submit(customerEmail, requested);
  }

  @Transactional(readOnly = true)
  public List<StoreOrder> getAllOrdersByClient(String email) {
    return forCustomer(email);
  }

  @Transactional(readOnly = true)
  public List<StoreOrder> getAllOrdersByEmployee(String email) {
    return forEmployee(email);
  }

  public StoreOrder confirm(long id, String employeeEmail) {
    StoreOrder order =
        orders.findById(id).orElseThrow(() -> new NotFoundException("error.orderNotFound"));
    Account employee =
        accounts
            .findByEmailIgnoreCase(employeeEmail)
            .orElseThrow(() -> new NotFoundException("error.employeeNotFound"));
    order.confirm(employee);
    log.info("Order {} confirmed by {}", id, employeeEmail);
    return order;
  }

  @Transactional(readOnly = true)
  public List<StoreOrder> forCustomer(String email) {
    return loadDetailed(orders.findIdsForCustomer(email));
  }

  @Transactional(readOnly = true)
  public List<StoreOrder> forEmployee(String email) {
    return loadDetailed(orders.findIdsForEmployee(email));
  }

  @Transactional(readOnly = true)
  public List<StoreOrder> all() {
    return loadDetailed(orders.findAllIdsSorted());
  }

  private List<StoreOrder> loadDetailed(List<Long> ids) {
    if (ids.isEmpty()) return List.of();
    return orders.findAllByIdInOrderByCreatedAtDesc(ids);
  }
}
