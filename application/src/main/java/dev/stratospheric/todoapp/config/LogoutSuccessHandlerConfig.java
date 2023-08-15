package dev.stratospheric.todoapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

/**
 * [N]:logout - For logging out the end user at the identity provider.
 */
@Configuration
public class LogoutSuccessHandlerConfig {

  /**
   * [N]:cognito - Defines the {@link CognitoOidcLogoutSuccessHandler} as a bean for when {@code custom.use-cognito-as-identity-provider} is set to {@code true} and inject the relevant credentials and region to configure the handler.<p/>
   * The ${COGNITO_...} environment variables are specified when deploying our Todo application with ECS (see {@link dev.stratospheric.todoapp.cdk.ServiceApp#environmentVariables}).
   * @param clientId
   * @param userPoolLogoutUrl
   * @return
   */
  @Bean
  @ConditionalOnProperty(prefix = "custom", name = "use-cognito-as-identity-provider", havingValue = "true")
  public LogoutSuccessHandler cognitoOidcLogoutSuccessHandler(
    @Value("${COGNITO_CLIENT_ID}") String clientId,
    @Value("${COGNITO_LOGOUT_URL}") String userPoolLogoutUrl)
  {
    return new CognitoOidcLogoutSuccessHandler(userPoolLogoutUrl, clientId);
  }

  /**
   * [N]:local - If running locally, we instantiate Spring Security’s OidcClientInitiatedLogoutSuccessHandler as Keycloak already supports the RP-initiated logout specification
   */
  @Bean
  @ConditionalOnProperty(prefix = "custom", name = "use-cognito-as-identity-provider", havingValue = "false")
  public LogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository clientRegistrationRepository) {
    OidcClientInitiatedLogoutSuccessHandler successHandler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
    successHandler.setPostLogoutRedirectUri("{baseUrl}");
    return successHandler;
  }
}
