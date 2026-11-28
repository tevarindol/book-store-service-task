package dev.is_a.tevarindol.bookstore.order;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OrderControllerTests {
  @Test
  void showsOrderHistoryForCurrentCustomer() throws Exception {
    String email = "reader@example.com";
    var authentication = UsernamePasswordAuthenticationToken.authenticated(email, null, List.of());
    OrderService orders = mock(OrderService.class);
    var history = List.of(mock(StoreOrder.class));
    when(orders.forCustomer(email)).thenReturn(history);
    var mvc = MockMvcBuilders.standaloneSetup(new OrderController(orders)).build();

    mvc.perform(get("/orders").principal(authentication))
        .andExpect(status().isOk())
        .andExpect(view().name("orders/list"))
        .andExpect(model().attribute("orders", history));

    verify(orders).forCustomer(email);
  }
}
