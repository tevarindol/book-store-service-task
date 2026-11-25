package dev.is_a.tevarindol.bookstore.client;

import dev.is_a.tevarindol.bookstore.account.Account;
import dev.is_a.tevarindol.bookstore.account.AccountForm;
import dev.is_a.tevarindol.bookstore.account.AccountRole;
import dev.is_a.tevarindol.bookstore.account.AccountService;
import dev.is_a.tevarindol.bookstore.common.NotFoundException;
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

  public Account getClientByEmail(String email) {
    Account account = accounts.getByEmail(email);
    if (account.getRole() != AccountRole.CUSTOMER)
      throw new NotFoundException("error.customerNotFound");
    return account;
  }

  public Account addClient(AccountForm form) {
    return accounts.register(form);
  }

  public Account updateClientByEmail(
      String email, String firstName, String lastName, String phone) {
    getClientByEmail(email);
    return accounts.updateProfileFields(email, firstName, lastName, phone);
  }

  public void deleteClientByEmail(String email) {
    accounts.deleteCustomer(email);
  }

  public void blockClient(long id, boolean blocked) {
    accounts.setBlocked(id, blocked);
  }
}
