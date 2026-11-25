package dev.is_a.tevarindol.bookstore.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MappingConfig {
  @Bean
  ModelMapper modelMapper() {
    return new ModelMapper();
  }
}
