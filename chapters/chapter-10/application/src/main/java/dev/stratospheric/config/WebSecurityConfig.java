package dev.stratospheric.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

/**
 * [N]:web-filter - Establishes rules for public and private access to the site.
 */
@Configuration
public class WebSecurityConfig {

  /**
   * [N]:logout - For enabling the OIDC standard of logging out the end user at the identity provider and from the web session.
   * @see LogoutSuccessHandlerConfig
   */
  private final LogoutSuccessHandler logoutSuccessHandler;

  public WebSecurityConfig(LogoutSuccessHandler logoutSuccessHandler) {
    this.logoutSuccessHandler = logoutSuccessHandler;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
    httpSecurity
      // Enables CSRF protection (to prevent a Cross-Site-Request-Forgery attack).
      .csrf()
      .and()
      // Configures the authentication support for OIDC. With oauth2Login, Spring Security refers to either OAuth 2.0 and/or OpenID Connect 1.0 authentication. 
      .oauth2Login()
      .and()

      // Permits any request to our static resources and public endpoints/views. 
      .authorizeRequests()
      .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
      .requestMatchers("/", "/health", "/register").permitAll()
      .anyRequest().authenticated()

      .and()
      .logout()
      // [N] To make our HttpSecurity configuration aware of this logout handler
      .logoutSuccessHandler(logoutSuccessHandler);

    return httpSecurity.build();
  }
}
