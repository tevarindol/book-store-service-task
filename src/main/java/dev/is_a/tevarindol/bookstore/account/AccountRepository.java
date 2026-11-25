package dev.is_a.tevarindol.bookstore.account;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
  Optional<Account> findByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCase(String email);

  List<Account> findByRoleOrderByLastNameAscFirstNameAsc(AccountRole role);
}
