package dev.is_a.tevarindol.bookstore.common;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
@Order(-1)
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {
  private final MessageSource messages;

  @ExceptionHandler(NotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public String notFound(NotFoundException exception, Model model, Locale locale) {
    model.addAttribute("status", 404);
    model.addAttribute("message", message(exception.getMessage(), locale));
    return "error/error";
  }

  @ExceptionHandler(NoResourceFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public String noResourceFound(Model model, Locale locale) {
    model.addAttribute("status", 404);
    model.addAttribute("message", message("error.notFound", locale));
    return "error/error";
  }

  @ExceptionHandler({BadRequestException.class, MethodArgumentNotValidException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public String badRequest(Exception exception, Model model, Locale locale) {
    model.addAttribute("status", 400);
    model.addAttribute(
        "message",
        exception instanceof BadRequestException
            ? message(exception.getMessage(), locale)
            : message("error.validation", locale));
    return "error/error";
  }

  @ExceptionHandler({ConflictException.class, DataIntegrityViolationException.class})
  @ResponseStatus(HttpStatus.CONFLICT)
  public String conflict(Exception exception, Model model, Locale locale) {
    model.addAttribute("status", 409);
    model.addAttribute(
        "message",
        exception instanceof ConflictException
            ? message(exception.getMessage(), locale)
            : message("error.conflict", locale));
    return "error/error";
  }

  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public String forbidden(Model model, Locale locale) {
    model.addAttribute("status", 403);
    model.addAttribute("message", message("error.forbidden", locale));
    return "error/error";
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public String unexpected(
      Exception exception, Model model, HttpServletRequest request, Locale locale) {
    log.error("Request failed: {} {}", request.getMethod(), request.getRequestURI(), exception);
    model.addAttribute("status", 500);
    model.addAttribute("message", message("error.unexpected", locale));
    return "error/error";
  }

  private String message(String code, Locale locale) {
    return messages.getMessage(code, null, code, locale);
  }
}
