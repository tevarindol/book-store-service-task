package dev.is_a.tevarindol.bookstore.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import jakarta.servlet.RequestDispatcher;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ErrorPageControllerTests {
  private static final Locale UKRAINIAN = Locale.forLanguageTag("uk");

  private final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(new ErrorPageController(errorMessages())).build();

  @ParameterizedTest
  @CsvSource({
    "400, Bad request",
    "403, Access forbidden",
    "404, Page not found",
    "503, Unexpected error"
  })
  void showsMessageForServletErrorStatus(int status, String message) throws Exception {
    mvc.perform(
            get("/error")
                .locale(Locale.ENGLISH)
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, status))
        .andExpect(view().name("error/error"))
        .andExpect(model().attribute("status", status))
        .andExpect(model().attribute("message", message));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = "404")
  void defaultsToInternalErrorForMissingOrNonIntegerStatus(String invalidStatus) throws Exception {
    var request = get("/error").locale(Locale.ENGLISH);
    if (invalidStatus != null) {
      request.requestAttr(RequestDispatcher.ERROR_STATUS_CODE, invalidStatus);
    }

    mvc.perform(request)
        .andExpect(view().name("error/error"))
        .andExpect(model().attribute("status", 500))
        .andExpect(model().attribute("message", "Unexpected error"));
  }

  @Test
  void forbiddenRouteUsesRequestLocale() throws Exception {
    mvc.perform(get("/error/403").locale(UKRAINIAN))
        .andExpect(view().name("error/error"))
        .andExpect(model().attribute("status", 403))
        .andExpect(model().attribute("message", "Доступ заборонено"));
  }

  private static StaticMessageSource errorMessages() {
    var messages = new StaticMessageSource();
    messages.addMessage("error.badRequest", Locale.ENGLISH, "Bad request");
    messages.addMessage("error.forbidden", Locale.ENGLISH, "Access forbidden");
    messages.addMessage("error.notFound", Locale.ENGLISH, "Page not found");
    messages.addMessage("error.unexpected", Locale.ENGLISH, "Unexpected error");
    messages.addMessage("error.forbidden", UKRAINIAN, "Доступ заборонено");
    return messages;
  }
}
