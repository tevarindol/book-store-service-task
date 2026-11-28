package dev.is_a.tevarindol.bookstore.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.is_a.tevarindol.bookstore.account.Account;
import dev.is_a.tevarindol.bookstore.account.AccountRepository;
import dev.is_a.tevarindol.bookstore.account.AccountRole;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class SecurityConfigTests {
  private static final String CUSTOMER_EMAIL = "security-reader@example.com";
  private static final String EMPLOYEE_EMAIL = "employee@bookstore.local";

  @Autowired private AccountRepository accounts;
  @Autowired private JwtProperties jwtProperties;
  @Autowired private JwtTokenService tokens;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private UserDetailsService users;

  private MockMvc mvc;

  @BeforeEach
  void setUp(
      @Autowired WebApplicationContext context,
      @Autowired @Qualifier("springSecurityFilterChain") Filter securityFilterChain) {
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilterChain).build();
  }

  @Test
  void guestCanBrowseBooksButMustSignInToSeeOrders() throws Exception {
    mvc.perform(get("/books")).andExpect(status().isOk());
    mvc.perform(get("/orders")).andExpect(redirectedUrl("/login"));
  }

  @Test
  @Transactional
  void customerCanSeeOrdersButCannotAccessEmployeeDashboard() throws Exception {
    accounts.save(
        new Account(CUSTOMER_EMAIL, "unused-hash", "Ada", "Reed", null, AccountRole.CUSTOMER));
    Cookie customer = authCookie(CUSTOMER_EMAIL);

    mvc.perform(get("/orders").cookie(customer)).andExpect(status().isOk());
    mvc.perform(get("/employee").cookie(customer))
        .andExpect(status().isForbidden())
        .andExpect(forwardedUrl("/error/403"));
  }

  @Test
  void employeeCanAccessEmployeeDashboard() throws Exception {
    mvc.perform(get("/employee").cookie(authCookie(EMPLOYEE_EMAIL))).andExpect(status().isOk());
  }

  @Test
  @Transactional
  void loginAuthenticatesCredentialsAndIssuesUsableCookie() throws Exception {
    String password = "test-password";
    accounts.save(
        new Account(
            CUSTOMER_EMAIL,
            passwordEncoder.encode(password),
            "Ada",
            "Reed",
            null,
            AccountRole.CUSTOMER));

    var login =
        mvc.perform(
                postWithCsrf("/login", "/login")
                    .param("username", "SECURITY-READER@example.com")
                    .param("password", password))
            .andExpect(redirectedUrl("/books"))
            .andExpect(cookie().exists(jwtProperties.cookieName()))
            .andReturn();

    Cookie auth = login.getResponse().getCookie(jwtProperties.cookieName());
    mvc.perform(get("/orders").cookie(auth)).andExpect(status().isOk());
  }

  @Test
  void invalidJwtCookieIsCleared() throws Exception {
    assertRejectedAuthCookie(new Cookie(jwtProperties.cookieName(), "invalid-token"));
  }

  @Test
  void jwtForMissingAccountIsRejected() throws Exception {
    String email = "missing-security-reader@example.com";
    assertThatThrownBy(() -> users.loadUserByUsername(email))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessage("error.accountNotFound");

    assertRejectedAuthCookie(authCookie(email));
  }

  @Test
  @Transactional
  void jwtIssuedBeforeAccountWasBlockedIsRejected() throws Exception {
    Account customer =
        accounts.save(
            new Account(CUSTOMER_EMAIL, "unused-hash", "Ada", "Reed", null, AccountRole.CUSTOMER));
    Cookie auth = authCookie(CUSTOMER_EMAIL);
    customer.setBlocked(true);
    accounts.flush();

    assertRejectedAuthCookie(auth);
  }

  @Test
  void blockingMissingCustomerReturnsLocalizedNotFound() throws Exception {
    mvc.perform(
            postWithCsrf("/employee/customers/-1/block", "/employee", authCookie(EMPLOYEE_EMAIL))
                .param("lang", "uk"))
        .andExpect(status().isNotFound())
        .andExpect(model().attribute("message", "Покупця не знайдено."));
  }

  @Test
  void logoutRequiresCsrfTokenAndClearsAuthCookie() throws Exception {
    Cookie auth = authCookie(EMPLOYEE_EMAIL);
    mvc.perform(post("/logout").cookie(auth))
        .andExpect(status().isForbidden())
        .andExpect(cookie().doesNotExist(jwtProperties.cookieName()));

    mvc.perform(postWithCsrf("/logout", "/employee", auth))
        .andExpect(redirectedUrl("/books?logout"))
        .andExpect(cookie().value(jwtProperties.cookieName(), ""))
        .andExpect(cookie().maxAge(jwtProperties.cookieName(), 0));
  }

  private void assertRejectedAuthCookie(Cookie auth) throws Exception {
    mvc.perform(get("/orders").cookie(auth))
        .andExpect(redirectedUrl("/login"))
        .andExpect(cookie().value(jwtProperties.cookieName(), ""))
        .andExpect(cookie().maxAge(jwtProperties.cookieName(), 0));
  }

  private MockHttpServletRequestBuilder postWithCsrf(
      String path, String pagePath, Cookie... cookies) throws Exception {
    var getPage = get(pagePath);
    var submit = post(path);
    if (cookies.length > 0) {
      getPage.cookie(cookies);
      submit.cookie(cookies);
    }
    var page =
        mvc.perform(getPage)
            .andExpect(status().isOk())
            .andExpect(cookie().exists("XSRF-TOKEN"))
            .andReturn();
    CsrfToken csrf = (CsrfToken) page.getRequest().getAttribute(CsrfToken.class.getName());
    Cookie csrfCookie = page.getResponse().getCookie("XSRF-TOKEN");

    return submit.cookie(csrfCookie).param(csrf.getParameterName(), csrf.getToken());
  }

  private Cookie authCookie(String email) {
    var authentication = UsernamePasswordAuthenticationToken.authenticated(email, null, List.of());
    return new Cookie(jwtProperties.cookieName(), tokens.issue(authentication));
  }
}
