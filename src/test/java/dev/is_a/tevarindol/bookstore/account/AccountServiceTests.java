package dev.is_a.tevarindol.bookstore.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.is_a.tevarindol.bookstore.common.BadRequestException;
import dev.is_a.tevarindol.bookstore.common.ConflictException;
import dev.is_a.tevarindol.bookstore.common.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class AccountServiceTests {
  private static final String PASSWORD = "vivid lunar orchard velvet compass 47";

  @Autowired private AccountService service;
  @Autowired private AccountRepository accounts;
  @Autowired private PasswordEncoder passwordEncoder;

  private final List<Long> createdAccountIds = new ArrayList<>();

  @AfterEach
  void removeTestAccounts() {
    accounts.deleteAllById(createdAccountIds);
  }

  @ParameterizedTest
  @EnumSource(AccountRole.class)
  void registrationPersistsAccountDetailsAndCreationTime(AccountRole role) {
    Account registered = register("  New.Account@Example.com  ", role);

    Account stored = reload(registered);
    assertThat(stored.getEmail()).isEqualTo("new.account@example.com");
    assertThat(stored.getRole()).isEqualTo(role);
    assertThat(stored.getPasswordHash()).isNotEqualTo(PASSWORD);
    assertThat(passwordEncoder.matches(PASSWORD, stored.getPasswordHash())).isTrue();
    assertThat(registered.getCreatedAt()).isNotNull();
    assertThat(stored.getCreatedAt()).isNotNull();
  }

  @Test
  void registrationRejectsDuplicateEmailAcrossRolesAndCase() {
    register("duplicate@example.com", AccountRole.CUSTOMER);

    assertThatThrownBy(() -> register("DUPLICATE@example.com", AccountRole.EMPLOYEE))
        .isInstanceOf(ConflictException.class)
        .hasMessage("error.duplicateAccount");
  }

  @Test
  void profileUpdatePersistsTrimmedNamesAndPhone() {
    Account customer = register("profile@example.com", AccountRole.CUSTOMER);

    service.updateProfileFields(customer.getEmail(), "  New first  ", "  New last  ", "+38012345");

    Account stored = reload(customer);
    assertThat(stored.getFirstName()).isEqualTo("New first");
    assertThat(stored.getLastName()).isEqualTo("New last");
    assertThat(stored.getPhone()).isEqualTo("+38012345");
    assertThat(stored.getEmail()).isEqualTo(customer.getEmail());
    assertThat(stored.getRole()).isEqualTo(customer.getRole());
    assertThat(stored.getPasswordHash()).isEqualTo(customer.getPasswordHash());
  }

  @Test
  void updateProfileUsesFormFields() {
    Account customer = register("profile-form@example.com", AccountRole.CUSTOMER);
    AccountForm form = new AccountForm();
    form.setFirstName("  New first  ");
    form.setLastName("  New last  ");
    form.setPhone("+38012345");

    Account updated = service.updateProfile(customer.getEmail(), form);

    Account stored = reload(customer);
    assertThat(updated.getId()).isEqualTo(customer.getId());
    assertThat(stored.getFirstName()).isEqualTo("New first");
    assertThat(stored.getLastName()).isEqualTo("New last");
    assertThat(stored.getPhone()).isEqualTo("+38012345");
  }

  @Test
  void getByEmailIgnoresCase() {
    Account customer = register("lookup@example.com", AccountRole.CUSTOMER);

    assertThat(service.getByEmail("LOOKUP@Example.com").getId()).isEqualTo(customer.getId());
  }

  @Test
  void getByEmailRejectsMissingAccount() {
    assertThatThrownBy(() -> service.getByEmail("missing-account@example.com"))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("error.accountNotFound");
  }

  @ParameterizedTest
  @EnumSource(AccountRole.class)
  void listsFilterByRoleAndSortByLastThenFirstName(AccountRole role) {
    Account last = register("list-last@example.com", role, "Amy", "Zimmer");
    Account second = register("list-second@example.com", role, "Zoe", "Baker");
    Account first = register("list-first@example.com", role, "Mia", "Baker");
    AccountRole otherRole =
        role == AccountRole.CUSTOMER ? AccountRole.EMPLOYEE : AccountRole.CUSTOMER;
    register("list-other-role@example.com", otherRole);

    List<Account> listed = role == AccountRole.CUSTOMER ? service.customers() : service.employees();

    assertThat(listed).extracting(Account::getRole).containsOnly(role);
    assertThat(listed)
        .extracting(Account::getId)
        .containsSubsequence(first.getId(), second.getId(), last.getId());
  }

  @Test
  void customerCanBeBlockedAndUnblocked() {
    Account customer = register("block-customer@example.com", AccountRole.CUSTOMER);

    service.setBlocked(customer.getId(), true);
    assertThat(reload(customer).isBlocked()).isTrue();

    service.setBlocked(customer.getId(), false);
    assertThat(reload(customer).isBlocked()).isFalse();
  }

  @Test
  void employeeCannotBeBlocked() {
    Account employee = register("block-employee@example.com", AccountRole.EMPLOYEE);

    assertThatThrownBy(() -> service.setBlocked(employee.getId(), true))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("error.blockCustomerOnly");
    assertThat(reload(employee).isBlocked()).isFalse();
  }

  @Test
  void setBlockedRejectsMissingAccount() {
    assertThatThrownBy(() -> service.setBlocked(-1L, true))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("error.accountNotFound");
  }

  @Test
  void deleteCustomerRejectsEmployeeAndRemovesCustomer() {
    Account employee = register("keep-employee@example.com", AccountRole.EMPLOYEE);
    Account customer = register("delete-customer@example.com", AccountRole.CUSTOMER);

    assertThatThrownBy(() -> service.deleteCustomer(employee.getEmail()))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("error.deleteCustomerOnly");
    assertThat(accounts.existsById(employee.getId())).isTrue();

    service.deleteCustomer(customer.getEmail());
    assertThat(accounts.existsById(customer.getId())).isFalse();
  }

  @Test
  void deleteEmployeeRejectsCustomerAndRemovesEmployee() {
    Account customer = register("keep-customer@example.com", AccountRole.CUSTOMER);
    Account employee = register("delete-employee@example.com", AccountRole.EMPLOYEE);

    assertThatThrownBy(() -> service.deleteEmployee(customer.getEmail()))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("error.employeeNotFound");
    assertThat(accounts.existsById(customer.getId())).isTrue();

    service.deleteEmployee(employee.getEmail());
    assertThat(accounts.existsById(employee.getId())).isFalse();
  }

  private Account register(String email, AccountRole role) {
    return register(email, role, "First", "Last");
  }

  private Account register(String email, AccountRole role, String firstName, String lastName) {
    AccountForm form = new AccountForm();
    form.setEmail(email);
    form.setPassword(PASSWORD);
    form.setFirstName(firstName);
    form.setLastName(lastName);
    Account account =
        role == AccountRole.CUSTOMER ? service.register(form) : service.registerEmployee(form);
    createdAccountIds.add(account.getId());
    return account;
  }

  private Account reload(Account account) {
    return accounts.findById(account.getId()).orElseThrow();
  }
}
