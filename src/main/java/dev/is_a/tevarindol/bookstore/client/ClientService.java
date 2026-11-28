package dev.is_a.tevarindol.bookstore.client;

import dev.is_a.tevarindol.bookstore.account.Account;
import dev.is_a.tevarindol.bookstore.account.AccountService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClientService {
  private final AccountService accounts;

  public List<Account> getAllClients() {
    return accounts.customers();
  }

  public void blockClient(long id, boolean blocked) {
    accounts.setBlocked(id, blocked);
  }
}
