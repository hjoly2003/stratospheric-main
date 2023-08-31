package dev.stratospheric.todoapp.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * [N]:logout]:cognito - Implements the {@link SimpleUrlLogoutSuccessHandler} interface for logging out a user from her Cognito session.<p/>
 */
public class CognitoOidcLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

  private final String logoutUrl;
  private final String clientId;

  /**
   * [N] We make both the {@code logoutUrl} (representing the logout endpoint of our Cognito instance, e.g., https://stratospheric...amazoncognito/logout) and the {@code clientId} (the app client id of our Todo application) configurable, as these depend on the actual Cognito instance.
   * @param logoutUrl
   * @param clientId
   */
  public CognitoOidcLogoutSuccessHandler(String logoutUrl, String clientId) {
    this.logoutUrl = logoutUrl;
    this.clientId = clientId;
  }

  /**
   * @return [N] the target URL that Cognito will redirect the end user to after the logout.
   */
  @Override
  protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) {

    UriComponents baseUrl = UriComponentsBuilder
      .fromHttpUrl(UrlUtils.buildFullRequestUrl(request))
      .replacePath(request.getContextPath())
      .replaceQuery(null)
      .fragment(null)
      .build();

    return UriComponentsBuilder
      .fromUri(URI.create(logoutUrl))
      .queryParam("client_id", clientId)
      // [N] The logout_uri parameter is the URL that Cognito will redirect the end user to after the logout. This has to be a valid URL that was configured as part of the LogoutURLs of the app client.
      .queryParam("logout_uri", baseUrl)
      .encode(StandardCharsets.UTF_8)
      .build()
      .toUriString();
  }
}
