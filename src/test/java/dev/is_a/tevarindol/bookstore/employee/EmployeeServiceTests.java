package dev.is_a.tevarindol.bookstore.employee;

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

class EmployeeServiceTests {
  private static final String EMAIL = "employee@example.com";

  private final AccountService accounts = mock(AccountService.class);
  private final EmployeeService employees = new EmployeeService(accounts);
  private final Account employee =
      new Account(EMAIL, "password-hash", "Ada", "Reed", null, AccountRole.EMPLOYEE);

  @Test
  void listsEmployees() {
    when(accounts.employees()).thenReturn(List.of(employee));

    assertThat(employees.getAllEmployees()).containsExactly(employee);
  }

  @Test
  void findsEmployeeByEmail() {
    when(accounts.getByEmail(EMAIL)).thenReturn(employee);

    assertThat(employees.getEmployeeByEmail(EMAIL)).isSameAs(employee);
  }

  @Test
  void updatesProfileFieldsForEmployee() {
    Account updated =
        new Account(EMAIL, "password-hash", "Alice", "Baker", "+38012345", AccountRole.EMPLOYEE);
    when(accounts.getByEmail(EMAIL)).thenReturn(employee);
    when(accounts.updateProfileFields(EMAIL, "Alice", "Baker", "+38012345")).thenReturn(updated);

    assertThat(employees.updateEmployeeByEmail(EMAIL, "Alice", "Baker", "+38012345"))
        .isSameAs(updated);
  }

  @Test
  void rejectsCustomerLookupAndUpdate() {
    Account customer =
        new Account(EMAIL, "password-hash", "Ada", "Reed", null, AccountRole.CUSTOMER);
    when(accounts.getByEmail(EMAIL)).thenReturn(customer);

    assertThatThrownBy(() -> employees.getEmployeeByEmail(EMAIL))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("error.employeeNotFound");
    assertThatThrownBy(() -> employees.updateEmployeeByEmail(EMAIL, "Alice", "Baker", "+38012345"))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("error.employeeNotFound");

    verify(accounts, times(2)).getByEmail(EMAIL);
    verifyNoMoreInteractions(accounts);
  }

  @Test
  void propagatesMissingAccountWithoutUpdating() {
    NotFoundException missing = new NotFoundException("error.accountNotFound");
    when(accounts.getByEmail(EMAIL)).thenThrow(missing);

    assertThatThrownBy(() -> employees.getEmployeeByEmail(EMAIL)).isSameAs(missing);
    assertThatThrownBy(() -> employees.updateEmployeeByEmail(EMAIL, "Alice", "Baker", "+38012345"))
        .isSameAs(missing);

    verify(accounts, times(2)).getByEmail(EMAIL);
    verifyNoMoreInteractions(accounts);
  }

  @Test
  void registersEmployeeWithSuppliedForm() {
    AccountForm form = new AccountForm();
    when(accounts.registerEmployee(form)).thenReturn(employee);

    assertThat(employees.addEmployee(form)).isSameAs(employee);
  }

  @Test
  void deletesEmployeeByEmail() {
    employees.deleteEmployeeByEmail(EMAIL);

    verify(accounts).deleteEmployee(EMAIL);
  }
}
