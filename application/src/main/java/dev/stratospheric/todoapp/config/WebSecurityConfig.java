package dev.stratospheric.todoapp.config;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import static org.springframework.security.config.Customizer.withDefaults;

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
      .csrf(
        csrf -> csrf.ignoringRequestMatchers(
          // [N]:websocket]:relay - Ensures that WebSocket connections to our relay don’t require the user of our sample Todo application to sign in first.
          "/stratospheric-todo-updates/**",
          "/websocket/**")
      )
      // Configures the authentication support for OIDC. With oauth2Login, Spring Security refers to either OAuth 2.0 and/or OpenID Connect 1.0 authentication. 
      .oauth2Login(withDefaults())

      // Permits any request to our static resources and public endpoints/views. 
      .authorizeHttpRequests(httpRequests -> httpRequests
        .requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
        .requestMatchers("/", "/register").permitAll()
        .anyRequest().authenticated())

      // [N] To make our HttpSecurity configuration aware of this logout handler
      .logout(logout -> logout.logoutSuccessHandler(logoutSuccessHandler));

    return httpSecurity.build();
  }
}
