package dev.stratospheric.todoapp.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

/**
 * [N]:cognito - For communicating with Cognito, we need to define this Spring Bean as this client is not auto-configured by Spring Cloud AWS for us.<p/>
 * As outlined in the chapter <em>Local Development</em>, we don’t want to connect to the real Cognito service when we’re working locally, so this Spring bean will not be activated if our custom property {@code use-cognito-as-identity-provider} is set to false.
 */
@Configuration
public class AwsConfig {

  @Bean
  @ConditionalOnProperty(prefix = "custom", name = "use-cognito-as-identity-provider", havingValue = "true")
  public CognitoIdentityProviderClient cognitoIdentityProviderClient(
    AwsRegionProvider regionProvider,
    AwsCredentialsProvider awsCredentialsProvider) {
    return CognitoIdentityProviderClient.builder()
      .credentialsProvider(awsCredentialsProvider)
      .region(regionProvider.getRegion())
      .build();
  }
}
