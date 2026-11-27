package dev.is_a.tevarindol.bookstore.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import dev.is_a.tevarindol.bookstore.common.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class RegistrationControllerTests {
  private static final String EMAIL = "reader@example.com";
  private static final String PASSWORD = "vivid lunar orchard velvet compass 47";

  @MockitoBean private AccountService accounts;

  private MockMvc mvc;

  @BeforeEach
  void setUp(@Autowired WebApplicationContext context) {
    mvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  @Test
  void showsLoginPage() throws Exception {
    mvc.perform(get("/login")).andExpect(view().name("auth/login"));
  }

  @Test
  void showsRegistrationPageWithNewForm() throws Exception {
    mvc.perform(get("/register"))
        .andExpect(view().name("auth/register"))
        .andExpect(model().attributeExists("accountForm"));
  }

  @Test
  void preservesExistingRegistrationForm() throws Exception {
    AccountForm form = new AccountForm();
    form.setEmail(EMAIL);

    mvc.perform(get("/register").flashAttr("accountForm", form))
        .andExpect(view().name("auth/register"))
        .andExpect(model().attribute("accountForm", form));
  }

  @Test
  void registersSubmittedDetailsAndRedirectsWithNotice() throws Exception {
    mvc.perform(registration(PASSWORD))
        .andExpect(redirectedUrl("/login"))
        .andExpect(flash().attribute("notice", "account.created"));

    ArgumentCaptor<AccountForm> form = ArgumentCaptor.forClass(AccountForm.class);
    verify(accounts).register(form.capture());
    assertThat(form.getValue().getEmail()).isEqualTo(EMAIL);
    assertThat(form.getValue().getPassword()).isEqualTo(PASSWORD);
    assertThat(form.getValue().getFirstName()).isEqualTo("Ada");
    assertThat(form.getValue().getLastName()).isEqualTo("Reed");
    assertThat(form.getValue().getPhone()).isEqualTo("+38012345");
  }

  @Test
  void weakPasswordShowsFieldErrorWithoutRegistering() throws Exception {
    mvc.perform(registration("Password123"))
        .andExpect(view().name("auth/register"))
        .andExpect(
            model().attributeHasFieldErrorCode("accountForm", "password", "PasswordStrength"));

    verifyNoInteractions(accounts);
  }

  @Test
  void duplicateEmailShowsFieldError() throws Exception {
    when(accounts.register(any(AccountForm.class)))
        .thenThrow(new ConflictException("error.duplicateAccount"));

    mvc.perform(registration(PASSWORD))
        .andExpect(view().name("auth/register"))
        .andExpect(
            model().attributeHasFieldErrorCode("accountForm", "email", "error.duplicateAccount"));
  }

  private MockHttpServletRequestBuilder registration(String password) {
    return post("/register")
        .param("email", EMAIL)
        .param("password", password)
        .param("firstName", "Ada")
        .param("lastName", "Reed")
        .param("phone", "+38012345");
  }
}
