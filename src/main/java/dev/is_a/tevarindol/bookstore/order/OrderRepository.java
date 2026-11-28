package dev.is_a.tevarindol.bookstore.order;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<StoreOrder, Long> {
  @Query(
      "SELECT o.id FROM StoreOrder o "
          + "WHERE LOWER(o.customer.email) = LOWER(:email) ORDER BY o.createdAt DESC")
  List<Long> findIdsForCustomer(@Param("email") String email);

  @Query("SELECT o.id FROM StoreOrder o ORDER BY o.createdAt DESC")
  List<Long> findAllIdsSorted();

  @EntityGraph(attributePaths = {"customer", "employee", "items", "items.book"})
  List<StoreOrder> findAllByIdInOrderByCreatedAtDesc(Collection<Long> ids);
}
