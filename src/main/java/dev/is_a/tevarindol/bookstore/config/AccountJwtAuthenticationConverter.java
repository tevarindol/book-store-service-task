package dev.is_a.tevarindol.bookstore.config;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountJwtAuthenticationConverter
    implements Converter<Jwt, AbstractAuthenticationToken> {
  private final UserDetailsService users;

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    UserDetails user = users.loadUserByUsername(jwt.getSubject());
    if (!user.isEnabled()) {
      throw new DisabledException("auth.accountDisabled");
    }
    return UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
  }
}
