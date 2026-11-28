package dev.is_a.tevarindol.bookstore.account;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AccountTests {
  private final Account account =
      new Account(
          "reader@example.com", "old-hash", "Ada", "Reed", "+38012345", AccountRole.CUSTOMER);

  @Test
  void updateProfileChangesNamesAndClearsPhoneWithoutChangingCredentials() {
    account.updateProfile("Alice", "Baker", null);

    assertThat(account.getFirstName()).isEqualTo("Alice");
    assertThat(account.getLastName()).isEqualTo("Baker");
    assertThat(account.getPhone()).isNull();
    assertThat(account.getEmail()).isEqualTo("reader@example.com");
    assertThat(account.getRole()).isEqualTo(AccountRole.CUSTOMER);
    assertThat(account.getPasswordHash()).isEqualTo("old-hash");
  }
}
