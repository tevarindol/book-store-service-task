package dev.is_a.tevarindol.bookstore.account;

import dev.is_a.tevarindol.bookstore.common.BadRequestException;
import dev.is_a.tevarindol.bookstore.common.ConflictException;
import dev.is_a.tevarindol.bookstore.common.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AccountService {
  private final AccountRepository accounts;
  private final PasswordEncoder passwordEncoder;

  public Account register(AccountForm form) {
    return createAccount(form, AccountRole.CUSTOMER);
  }

  public Account registerEmployee(AccountForm form) {
    return createAccount(form, AccountRole.EMPLOYEE);
  }

  private Account createAccount(AccountForm form, AccountRole role) {
    String email = form.getEmail().trim().toLowerCase();
    if (accounts.existsByEmailIgnoreCase(email))
      throw new ConflictException("error.duplicateAccount");
    Account account =
        new Account(
            email,
            passwordEncoder.encode(form.getPassword()),
            form.getFirstName().trim(),
            form.getLastName().trim(),
            form.getPhone(),
            role);
    return accounts.save(account);
  }

  public Account updateProfileFields(
      String email, String firstName, String lastName, String phone) {
    Account account = getByEmail(email);
    account.updateProfile(firstName.trim(), lastName.trim(), phone);
    return account;
  }

  @Transactional(readOnly = true)
  public Account getByEmail(String email) {
    return accounts
        .findByEmailIgnoreCase(email)
        .orElseThrow(() -> new NotFoundException("error.accountNotFound"));
  }

  @Transactional(readOnly = true)
  public List<Account> customers() {
    return accounts.findByRoleOrderByLastNameAscFirstNameAsc(AccountRole.CUSTOMER);
  }

  @Transactional(readOnly = true)
  public List<Account> employees() {
    return accounts.findByRoleOrderByLastNameAscFirstNameAsc(AccountRole.EMPLOYEE);
  }

  public void setBlocked(long id, boolean blocked) {
    Account account =
        accounts.findById(id).orElseThrow(() -> new NotFoundException("error.accountNotFound"));
    if (account.getRole() != AccountRole.CUSTOMER)
      throw new BadRequestException("error.blockCustomerOnly");
    account.setBlocked(blocked);
  }

  public void deleteCustomer(String email) {
    Account account = getByEmail(email);
    if (account.getRole() != AccountRole.CUSTOMER)
      throw new BadRequestException("error.deleteCustomerOnly");
    accounts.delete(account);
  }

  public void deleteEmployee(String email) {
    Account account = getByEmail(email);
    if (account.getRole() != AccountRole.EMPLOYEE)
      throw new NotFoundException("error.employeeNotFound");
    accounts.delete(account);
  }
}
