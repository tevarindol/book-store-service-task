package dev.is_a.tevarindol.bookstore.book;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookForm {
  @NotBlank
  @Size(max = 20)
  private String isbn;

  @NotBlank
  @Size(max = 255)
  private String title;

  @NotBlank
  @Size(max = 255)
  private String author;

  @NotBlank
  @Size(max = 4000)
  private String description;

  @NotNull
  @DecimalMin("0.01")
  private BigDecimal price;

  @PositiveOrZero private int stockQuantity;
  private boolean active = true;
}
