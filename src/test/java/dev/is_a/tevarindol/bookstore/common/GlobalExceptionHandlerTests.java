package dev.is_a.tevarindol.bookstore.common;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTests {
  private static final Locale UKRAINIAN = Locale.forLanguageTag("uk");

  private final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(new FailingController())
          .setControllerAdvice(new GlobalExceptionHandler(errorMessages()))
          .build();

  @ParameterizedTest
  @MethodSource("errors")
  void showsLocalizedErrorPage(Exception failure, int expectedStatus, String message)
      throws Exception {
    mvc.perform(get("/failure").locale(UKRAINIAN).requestAttr("failure", failure))
        .andExpect(status().is(expectedStatus))
        .andExpect(view().name("error/error"))
        .andExpect(model().attribute("status", expectedStatus))
        .andExpect(model().attribute("message", message));
  }

  private static Stream<Arguments> errors() throws NoSuchMethodException {
    var parameter =
        new MethodParameter(FailingController.class.getDeclaredMethod("fail", Exception.class), 0);
    return Stream.of(
        arguments(new NotFoundException("error.bookNotFound"), 404, "Книгу не знайдено"),
        arguments(
            new NoResourceFoundException(HttpMethod.GET, "/failure", "missing.css"),
            404,
            "Сторінку не знайдено"),
        arguments(new BadRequestException("error.emptyBasket"), 400, "Кошик порожній"),
        arguments(
            new MethodArgumentNotValidException(
                parameter, new BeanPropertyBindingResult(new Object(), "form")),
            400,
            "Перевірте введені дані"),
        arguments(new ConflictException("error.duplicateIsbn"), 409, "ISBN уже існує"),
        arguments(
            new DataIntegrityViolationException("Internal database details"),
            409,
            "Конфлікт даних"),
        arguments(new AccessDeniedException("Internal access details"), 403, "Доступ заборонено"),
        arguments(
            new IllegalStateException("Internal failure details"), 500, "Неочікувана помилка"));
  }

  private static StaticMessageSource errorMessages() {
    var messages = new StaticMessageSource();
    messages.addMessage("error.bookNotFound", UKRAINIAN, "Книгу не знайдено");
    messages.addMessage("error.notFound", UKRAINIAN, "Сторінку не знайдено");
    messages.addMessage("error.emptyBasket", UKRAINIAN, "Кошик порожній");
    messages.addMessage("error.validation", UKRAINIAN, "Перевірте введені дані");
    messages.addMessage("error.duplicateIsbn", UKRAINIAN, "ISBN уже існує");
    messages.addMessage("error.conflict", UKRAINIAN, "Конфлікт даних");
    messages.addMessage("error.forbidden", UKRAINIAN, "Доступ заборонено");
    messages.addMessage("error.unexpected", UKRAINIAN, "Неочікувана помилка");
    return messages;
  }

  @Controller
  class FailingController {
    @GetMapping("/failure")
    String fail(@RequestAttribute("failure") Exception failure) throws Exception {
      throw failure;
    }
  }
}
