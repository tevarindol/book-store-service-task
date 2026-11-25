package dev.is_a.tevarindol.bookstore.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
  @NotEmpty @Valid private List<OrderLineDTO> items;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class OrderLineDTO {
    @NotNull @Positive private Long bookId;

    @Positive private int quantity;
  }
}
