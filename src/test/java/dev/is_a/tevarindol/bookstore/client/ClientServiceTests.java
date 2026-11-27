package dev.is_a.tevarindol.bookstore.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import dev.is_a.tevarindol.bookstore.account.Account;
import dev.is_a.tevarindol.bookstore.account.AccountForm;
import dev.is_a.tevarindol.bookstore.account.AccountRole;
import dev.is_a.tevarindol.bookstore.account.AccountService;
import dev.is_a.tevarindol.bookstore.common.NotFoundException;
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

  @Test
  void findsCustomerByEmail() {
    when(accounts.getByEmail(EMAIL)).thenReturn(customer);

    assertThat(clients.getClientByEmail(EMAIL)).isSameAs(customer);
  }

  @Test
  void rejectsEmployeeLookupAndUpdate() {
    Account employee =
        new Account(EMAIL, "password-hash", "Ada", "Reed", null, AccountRole.EMPLOYEE);
    when(accounts.getByEmail(EMAIL)).thenReturn(employee);

    assertThatThrownBy(() -> clients.getClientByEmail(EMAIL))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("error.customerNotFound");
    assertThatThrownBy(() -> clients.updateClientByEmail(EMAIL, "Alice", "Baker", "+38012345"))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("error.customerNotFound");

    verify(accounts, times(2)).getByEmail(EMAIL);
    verifyNoMoreInteractions(accounts);
  }

  @Test
  void propagatesMissingAccountWithoutUpdating() {
    NotFoundException missing = new NotFoundException("error.accountNotFound");
    when(accounts.getByEmail(EMAIL)).thenThrow(missing);

    assertThatThrownBy(() -> clients.getClientByEmail(EMAIL)).isSameAs(missing);
    assertThatThrownBy(() -> clients.updateClientByEmail(EMAIL, "Alice", "Baker", "+38012345"))
        .isSameAs(missing);

    verify(accounts, times(2)).getByEmail(EMAIL);
    verifyNoMoreInteractions(accounts);
  }

  @Test
  void registersCustomerWithSuppliedForm() {
    AccountForm form = new AccountForm();
    when(accounts.register(form)).thenReturn(customer);

    assertThat(clients.addClient(form)).isSameAs(customer);
  }

  @Test
  void updatesProfileFieldsForCustomer() {
    Account updated =
        new Account(EMAIL, "password-hash", "Alice", "Baker", "+38012345", AccountRole.CUSTOMER);
    when(accounts.getByEmail(EMAIL)).thenReturn(customer);
    when(accounts.updateProfileFields(EMAIL, "Alice", "Baker", "+38012345")).thenReturn(updated);

    assertThat(clients.updateClientByEmail(EMAIL, "Alice", "Baker", "+38012345")).isSameAs(updated);
  }

  @Test
  void deletesCustomerByEmail() {
    clients.deleteClientByEmail(EMAIL);

    verify(accounts).deleteCustomer(EMAIL);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void passesRequestedBlockedStateToAccountService(boolean blocked) {
    clients.blockClient(42L, blocked);

    verify(accounts).setBlocked(42L, blocked);
  }
}
