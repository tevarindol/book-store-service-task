package dev.is_a.tevarindol.bookstore.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProfileControllerTests {
  private static final String EMAIL = "reader@example.com";

  private final Authentication authentication =
      UsernamePasswordAuthenticationToken.authenticated(EMAIL, null, List.of());
  private final Account account =
      new Account(EMAIL, "password-hash", "Ada", "Reed", "+38012345", AccountRole.CUSTOMER);
  private final AccountService accounts = mock(AccountService.class);
  private final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(new ProfileController(accounts, new ModelMapper())).build();

  @Test
  void showsCurrentAccountAndPrefillsProfileForm() throws Exception {
    when(accounts.getByEmail(EMAIL)).thenReturn(account);

    var result =
        mvc.perform(get("/profile").principal(authentication))
            .andExpect(view().name("account/profile"))
            .andExpect(model().attribute("account", account))
            .andReturn();

    assertThat(result.getModelAndView().getModel().get("profileForm"))
        .isInstanceOfSatisfying(
            ProfileForm.class,
            form -> {
              assertThat(form.getFirstName()).isEqualTo("Ada");
              assertThat(form.getLastName()).isEqualTo("Reed");
              assertThat(form.getPhone()).isEqualTo("+38012345");
            });
  }

  @Test
  void updatesCurrentAccountAndRedirectsToProfile() throws Exception {
    mvc.perform(
            post("/profile")
                .principal(authentication)
                .param("firstName", "Alice")
                .param("lastName", "Baker")
                .param("phone", "+38054321"))
        .andExpect(redirectedUrl("/profile?updated"));

    verify(accounts).updateProfileFields(EMAIL, "Alice", "Baker", "+38054321");
  }

  @Test
  void invalidFormShowsFieldErrorAndDoesNotUpdateAccount() throws Exception {
    when(accounts.getByEmail(EMAIL)).thenReturn(account);

    mvc.perform(
            post("/profile")
                .principal(authentication)
                .param("firstName", " ")
                .param("lastName", "Baker")
                .param("phone", "+38054321"))
        .andExpect(view().name("account/profile"))
        .andExpect(model().attribute("account", account))
        .andExpect(model().attributeHasFieldErrorCode("profileForm", "firstName", "NotBlank"));

    verify(accounts).getByEmail(EMAIL);
    verifyNoMoreInteractions(accounts);
  }

  @Test
  void deletesCurrentAccountInvalidatesSessionAndRedirectsToBooks() throws Exception {
    MockHttpSession session = new MockHttpSession();

    mvc.perform(post("/profile/delete").principal(authentication).session(session))
        .andExpect(redirectedUrl("/books?accountDeleted"));

    verify(accounts).deleteCustomer(EMAIL);
    assertThat(session.isInvalid()).isTrue();
  }
}
