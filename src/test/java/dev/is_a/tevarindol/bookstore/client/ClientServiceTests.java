package dev.is_a.tevarindol.bookstore.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.is_a.tevarindol.bookstore.account.Account;
import dev.is_a.tevarindol.bookstore.account.AccountRole;
import dev.is_a.tevarindol.bookstore.account.AccountService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ClientServiceTests {
  private static final String EMAIL = "reader@example.com";

  private final AccountService accounts = mock(AccountService.class);
  private final ClientService clients = new ClientService(accounts);
  private final Account customer =
      new Account(EMAIL, "password-hash", "Ada", "Reed", null, AccountRole.CUSTOMER);

  @Test
  void listsCustomers() {
    when(accounts.customers()).thenReturn(List.of(customer));

    assertThat(clients.getAllClients()).containsExactly(customer);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void passesRequestedBlockedStateToAccountService(boolean blocked) {
    clients.blockClient(42L, blocked);

    verify(accounts).setBlocked(42L, blocked);
  }
}
