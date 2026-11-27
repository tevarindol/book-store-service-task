package dev.is_a.tevarindol.bookstore.basket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

class BasketServiceTests {
  private static final String SESSION_KEY = "testBasket";

  private final BasketService baskets = new BasketService();
  private final MockHttpSession session = new MockHttpSession();

  @BeforeEach
  void configureSessionKey() {
    ReflectionTestUtils.setField(baskets, "sessionKey", SESSION_KEY);
  }

  @Test
  void createsAndReusesAnEmptyBasketPerSession() {
    var basket = baskets.get(session);

    assertThat(basket).isEmpty();
    assertThat(session.getAttribute(SESSION_KEY)).isSameAs(basket);
    assertThat(baskets.get(session)).isSameAs(basket);
    assertThat(baskets.get(new MockHttpSession())).isNotSameAs(basket).isEmpty();
  }

  @Test
  void addsBooksInInsertionOrderAndIncrementsExistingQuantity() {
    baskets.add(session, 42L);
    assertThat(baskets.get(session)).containsExactly(entry(42L, 1));

    baskets.add(session, 7L);
    baskets.add(session, 42L);

    assertThat(baskets.get(session)).containsExactly(entry(42L, 2), entry(7L, 1));
  }

  @Test
  void removesAllCopiesOfOneBookAndIgnoresMissingBooks() {
    baskets.add(session, 42L);
    baskets.add(session, 42L);
    baskets.add(session, 7L);

    baskets.remove(session, 99L);
    assertThat(baskets.get(session)).containsExactly(entry(42L, 2), entry(7L, 1));

    baskets.remove(session, 42L);
    assertThat(baskets.get(session)).containsExactly(entry(7L, 1));
  }

  @Test
  void clearsOnlyTheCurrentBasketAndAllowsNewItems() {
    MockHttpSession otherSession = new MockHttpSession();
    baskets.add(session, 42L);
    baskets.add(session, 7L);
    baskets.add(otherSession, 42L);

    baskets.clear(session);

    assertThat(baskets.get(session)).isEmpty();
    assertThat(baskets.get(otherSession)).containsExactly(entry(42L, 1));

    baskets.add(session, 42L);
    assertThat(baskets.get(session)).containsExactly(entry(42L, 1));
  }
}
