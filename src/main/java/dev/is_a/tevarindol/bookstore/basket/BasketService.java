package dev.is_a.tevarindol.bookstore.basket;

import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BasketService {
  @Value("${app.basket.session-attribute}")
  private String sessionKey;

  @SuppressWarnings("unchecked")
  public Map<Long, Integer> get(HttpSession session) {
    return Optional.ofNullable(session.getAttribute(sessionKey))
        .map(current -> (Map<Long, Integer>) current)
        .orElseGet(
            () -> {
              Map<Long, Integer> basket = new LinkedHashMap<>();
              session.setAttribute(sessionKey, basket);
              return basket;
            });
  }

  public void add(HttpSession session, long bookId) {
    get(session).merge(bookId, 1, Integer::sum);
  }

  public void remove(HttpSession session, long bookId) {
    get(session).remove(bookId);
  }

  public void clear(HttpSession session) {
    get(session).clear();
  }
}
