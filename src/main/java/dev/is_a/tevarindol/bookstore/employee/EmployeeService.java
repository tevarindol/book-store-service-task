package dev.is_a.tevarindol.bookstore.employee;

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
public class EmployeeService {
  private final AccountService accounts;

  public List<Account> getAllEmployees() {
    return accounts.employees();
  }

  public Account getEmployeeByEmail(String email) {
    Account account = accounts.getByEmail(email);
    if (account.getRole() != AccountRole.EMPLOYEE)
      throw new NotFoundException("error.employeeNotFound");
    return account;
  }

  public Account updateEmployeeByEmail(
      String email, String firstName, String lastName, String phone) {
    getEmployeeByEmail(email);
    return accounts.updateProfileFields(email, firstName, lastName, phone);
  }

  public Account addEmployee(AccountForm form) {
    return accounts.registerEmployee(form);
  }

  public void deleteEmployeeByEmail(String email) {
    accounts.deleteEmployee(email);
  }
}
