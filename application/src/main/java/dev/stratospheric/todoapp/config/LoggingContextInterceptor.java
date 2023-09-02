package dev.stratospheric.todoapp.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 *  [N]:cognito]:log]:custom-fields - Intercepts all incoming web requests to add the name of a user to each log event.
 */
class LoggingContextInterceptor implements HandlerInterceptor {

  private final Logger logger = LoggerFactory.getLogger(LoggingContextInterceptor.class);

  /**
   * Gets the logged-in user who initiated the request, and then add its name to the MDC.<p/>
 * The "Message Diagnostic Context" (or MDC in short) which is supported by SLF4J, the default logger of Spring Boot. MDC allows us to add custom fields to log events.
   */
  @Override
  public boolean preHandle(
    final HttpServletRequest request,
    final HttpServletResponse response,
    final Object handler) {

    // [N] Once a user logged in, Spring Security creates a Principal in the form of an OidcUser.
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String userId = getUserIdFromPrincipal(authentication.getPrincipal());
    // [N] Adds the username to the userId field in the MDC. 
    MDC.put("userId", userId);
    return true;
  }

  private String getUserIdFromPrincipal(Object principal) {
    if (principal instanceof String) {
      // anonymous users will have a String principal with value "anonymousUser"
      return principal.toString();
    }

    if (principal instanceof OidcUser) {
      try {
        OidcUser user = (OidcUser) principal;
        if (user.getPreferredUsername() != null) {
          return user.getPreferredUsername();
        } else if (user.getClaimAsString("name") != null) {
          return user.getClaimAsString("name");
        } else {
          logger.warn("could not extract userId from Principal");
          return "unknown";
        }
      } catch (Exception e) {
        logger.warn("could not extract userId from Principal", e);
      }
    }
    return "unknown";
  }

  /**
   * Removes the MDC once the request has been processed.<p/>
   * This removal is required since the MDC is attached to the current thread, and it might be reused later on (because it’s part of a thread pool).
   */
  @Override
  public void afterCompletion(
    final HttpServletRequest request,
    final HttpServletResponse response,
    final Object handler,
    final Exception ex) {
    MDC.clear();
  }
}
