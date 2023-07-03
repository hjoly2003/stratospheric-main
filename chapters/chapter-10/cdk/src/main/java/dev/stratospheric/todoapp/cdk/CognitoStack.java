package dev.stratospheric.todoapp.cdk;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import dev.stratospheric.cdk.ApplicationEnvironment;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.customresources.AwsCustomResource;
import software.amazon.awscdk.customresources.AwsCustomResourcePolicy;
import software.amazon.awscdk.customresources.AwsSdkCall;
import software.amazon.awscdk.customresources.PhysicalResourceId;
import software.amazon.awscdk.customresources.SdkCallsPolicyOptions;
import software.amazon.awscdk.services.cognito.AccountRecovery;
import software.amazon.awscdk.services.cognito.AutoVerifiedAttrs;
import software.amazon.awscdk.services.cognito.CognitoDomainOptions;
import software.amazon.awscdk.services.cognito.Mfa;
import software.amazon.awscdk.services.cognito.OAuthFlows;
import software.amazon.awscdk.services.cognito.OAuthScope;
import software.amazon.awscdk.services.cognito.OAuthSettings;
import software.amazon.awscdk.services.cognito.PasswordPolicy;
import software.amazon.awscdk.services.cognito.SignInAliases;
import software.amazon.awscdk.services.cognito.StandardAttribute;
import software.amazon.awscdk.services.cognito.StandardAttributes;
import software.amazon.awscdk.services.cognito.UserPool;
import software.amazon.awscdk.services.cognito.UserPoolClient;
import software.amazon.awscdk.services.cognito.UserPoolClientIdentityProvider;
import software.amazon.awscdk.services.cognito.UserPoolDomain;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

import static software.amazon.awscdk.customresources.AwsCustomResourcePolicy.ANY_RESOURCE;

/**
 * [N]:cognito - Deploys the following 
 * <ul>
 *  <li>a UserPool resource which is a user directory where we can store and manage user information.</li>
 *  <li>a User Pool Client which is associated with a User Pool and has permission to call unauthenticated API operations like signing in or registering users. Therefore, every App Client requires a client ID and an optional secret.</li>
 *  <li>a User Pool Domain which defines the url of the Cognito Identity Provider.</li>
 * </ul>
 * [N] Note: This level 2 construct configures Amazon Cognito as the email delivery service (for sending the password or a recovery email, for example) instead of Amazon SES. 
 */
public class CognitoStack extends Stack {

  private final ApplicationEnvironment applicationEnvironment;

  /** 
   * @see <a href="https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-cognito-userpool.html">AWS::Cognito::UserPool</a>
  */
  private final UserPool userPool;
  
  /**
   * @see <a href="https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-cognito-userpoolclient.html">AWS::Cognito::UserPoolclient</a>
   */
  private final UserPoolClient userPoolClient;
  
  /**
   * @see <a href="https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-cognito-userpooldomain.html">AWS::Cognito::UserPoolDomain</a>
   */
  private final UserPoolDomain userPoolDomain;
  private String userPoolClientSecret;
  private final String logoutUrl;

  public CognitoStack(
    final Construct scope,
    final String id,
    final Environment awsEnvironment,
    final ApplicationEnvironment applicationEnvironment,
    final CognitoInputParameters inputParameters) {

    super(scope, id, StackProps.builder()
      .stackName(applicationEnvironment.prefix("Cognito"))
      .env(awsEnvironment).build());

    this.applicationEnvironment = applicationEnvironment;

    // [N] Creates the User Pool resource.
    this.userPool = UserPool.Builder.create(this, "userPool")
      .userPoolName(inputParameters.applicationName + "-user-pool")
      // [N] Disables self-sign-up to only allow admin users to add new users to our User Pool. Our Todo application will act as such an admin and create our users. Hence, we have to extend the IAM role for our ECS task and allow our application to perform all operations related to the identity provider.
      .selfSignUpEnabled(false)
      .standardAttributes(StandardAttributes.builder()
        .email(StandardAttribute.builder().required(true).mutable(false).build())
        .build())
      .signInAliases(SignInAliases.builder().username(true).email(true).build())
      .signInCaseSensitive(true)
      .autoVerify(AutoVerifiedAttrs.builder().email(true).build())
      // [N] Multi-factor authentication (MFA) is disabled as we set mfa to OFF.
      .mfa(Mfa.OFF)
      .accountRecovery(AccountRecovery.EMAIL_ONLY)
      // [N] Sets the passwordPolicy to a solid security standard by requesting users to enter a password with at least twelve mixed (symbols, digits, uppercase, lowercase) characters.
      .passwordPolicy(PasswordPolicy.builder()
        .requireLowercase(true)
        .requireDigits(true)
        .requireSymbols(true)
        .requireUppercase(true)
        .minLength(12)
        .tempPasswordValidity(Duration.days(7))
        .build())
      .build();

    // [N] Adds a User Pool Client resouce for our application.
    this.userPoolClient = UserPoolClient.Builder.create(this, "userPoolClient")
      // [N] We define the client’s name (userPoolClientName) to avoid random client names. 
      .userPoolClientName(inputParameters.applicationName + "-client")
      // [N] For security reasons, AWS generates the secret for this client for us.
      .generateSecret(true)
      // [N] By setting userPool to the Java object that was returned when creating the UserPool we connect this client to our User Pool.
      .userPool(this.userPool)
      .oAuth(OAuthSettings.builder()
        // [N] Next, for both the callback and logout URLs, we’re adding two URLs. One URL is the final production URL, while the other one is used for local development and troubleshooting. The path /login/oauth2/code/cognito is the standard callback URI for Spring Security.
        .callbackUrls(Arrays.asList(
          String.format("%s/login/oauth2/code/cognito", inputParameters.applicationUrl),
          "http://localhost:8080/login/oauth2/code/cognito"
        ))
        .logoutUrls(Arrays.asList(inputParameters.applicationUrl, "http://localhost:8080"))
        // [N] Allows this user pool client to use the OAuth 2.0 Authorization Code Grant flow with a pre-defined set of OAuth 2.0 scopes.
        .flows(OAuthFlows.builder()
          .authorizationCodeGrant(true)
          .build())
        .scopes(Arrays.asList(OAuthScope.EMAIL, OAuthScope.OPENID, OAuthScope.PROFILE))
        .build())
      .supportedIdentityProviders(Collections.singletonList(UserPoolClientIdentityProvider.COGNITO))
      .build();

    // [N] Creates a User Pool Domain resource. As we are not using a custom domain for the sign-in page, we’re configuring the prefix for the Amazon Cognito domain. If we deploy this stack to the region eu-central-1 and use “stratospheric” as the LoginPageDomainPrefix, we will get the following sign-in URL: https://stratospheric.auth.eu-central-1-amazoncognito.com.
    this.userPoolDomain = UserPoolDomain.Builder.create(this, "userPoolDomain")
      .userPool(this.userPool)
      .cognitoDomain(CognitoDomainOptions.builder()
        .domainPrefix(inputParameters.loginPageDomainPrefix)
        .build())
      .build();

    this.logoutUrl = String.format("https://%s.auth.%s.amazoncognito.com/logout",
      inputParameters.loginPageDomainPrefix, awsEnvironment.getRegion());

    createOutputParameters(awsEnvironment);

    applicationEnvironment.tag(this);
  }

  private static final String PARAMETER_USER_POOL_ID = "userPoolId";
  private static final String PARAMETER_USER_POOL_CLIENT_ID = "userPoolClientId";
  private static final String PARAMETER_USER_POOL_CLIENT_SECRET = "userPoolClientSecret";
  private static final String PARAMETER_USER_POOL_LOGOUT_URL = "userPoolLogoutUrl";
  private static final String PARAMETER_USER_POOL_PROVIDER_URL = "userPoolProviderUrl";

  /**
   * Exposes the dynamic attributes (SSM parameters) of our Cognito setup that'll get used in the application's configuration of Spring Security.</p>
   * It uses the “parameter store” feature of the AWS Systems Manager (SSM). As our Spring Boot application expects these values on application startup, we have to deploy or update the Cognito stack before triggering a new ECS deployment of our Todo app.</p> 
   * Note: Since they're randomly generated, we need to expose the IDs of the UserPool, the UserPoolClient and of the UserPoolClientSecret to later fetch the secret of the Onuth 2 client.
   * @param awsEnvironment Used to pass in the region
   * @return <ul>
   *    <li><em>PARAMETER_USER_POOL_ID</em></li>
   *    <li><em>PARAMETER_USER_POOL_CLIENT_ID</em></li>
   *    <li><em>PARAMETER_USER_POOL_CLIENT_SECRET</em> Note, for sake of simplicity, this is not encrypted but rather stored in plain text. We could have stored that sensitive configuration value as SecureStrings inside the AWS Parameter Store and use Spring Cloud AWS to fetch the values upon application start (see https://rieckpil.de/resolving-spring-boot-properties-using-the-aws-parameter-store-ssm/).</li> 
   *    <li><em>PARAMETER_USER_POOL_LOGOUT_URL</em> Required for fully logging out the end user. This URL contains our loginDomainPrefix and the AWS region, which will look something like this: {@code https://stratospheric.auth.eu-central-1.amazoncognito.com/logout}</li>
   *    <li><em>PARAMETER_USER_POOL_PROVIDER_URL</em> Used in the configuration of Spring Security to discover the OAuth 2.0 relevant endpoints. This URL contains the AWS region and our user pool identifier and will look like this: {@code https://cognito-idp.eu-central-1.amazonaws.com/eu-central-1_pD8flsXa}</li>
   */
  private void createOutputParameters(Environment awsEnvironment) {

    StringParameter.Builder.create(this, "userPoolId")
      .parameterName(createParameterName(applicationEnvironment, PARAMETER_USER_POOL_ID))
      .stringValue(this.userPool.getUserPoolId())
      .build();

    StringParameter.Builder.create(this, "userPoolClientId")
      .parameterName(createParameterName(applicationEnvironment, PARAMETER_USER_POOL_CLIENT_ID))
      .stringValue(this.userPoolClient.getUserPoolClientId())
      .build();

    StringParameter.Builder.create(this, "logoutUrl")
      .parameterName(createParameterName(applicationEnvironment, PARAMETER_USER_POOL_LOGOUT_URL))
      .stringValue(this.logoutUrl)
      .build();

    StringParameter.Builder.create(this, "providerUrl")
      .parameterName(createParameterName(applicationEnvironment, PARAMETER_USER_POOL_PROVIDER_URL))
      .stringValue(this.userPool.getUserPoolProviderUrl())
      .build();

    // CloudFormation does not expose the UserPoolClient secret, so we can't access it directly with
    // CDK. As a workaround, we create a custom resource that calls the AWS API to get the secret, and
    // then store it in the parameter store like the other parameters.
    // Source: https://github.com/aws/aws-cdk/issues/7225
    AwsCustomResource describeUserPoolResource = AwsCustomResource.Builder.create(this, "describeUserPool")
      .resourceType("Custom::DescribeCognitoUserPoolClient")
      .onCreate(AwsSdkCall.builder()
        .region(awsEnvironment.getRegion())
        .service("CognitoIdentityServiceProvider")
        .action("describeUserPoolClient")
        .parameters(Map.of(
          "UserPoolId", this.userPool.getUserPoolId(),
          "ClientId", this.userPoolClient.getUserPoolClientId()
        ))
        .physicalResourceId(PhysicalResourceId.of(this.userPoolClient.getUserPoolClientId()))
        .build())
      .policy(AwsCustomResourcePolicy.fromSdkCalls(
        SdkCallsPolicyOptions.builder()
          .resources(ANY_RESOURCE)
          .build()
      ))
      .build();

    this.userPoolClientSecret = describeUserPoolResource.getResponseField("UserPoolClient.ClientSecret");

    StringParameter userPoolClientSecret = StringParameter.Builder.create(this, "userPoolClientSecret")
      .parameterName(createParameterName(applicationEnvironment, PARAMETER_USER_POOL_CLIENT_SECRET))
      .stringValue(this.userPoolClientSecret)
      .build();
  }

  private static String createParameterName(ApplicationEnvironment applicationEnvironment, String parameterName) {
    return applicationEnvironment.getEnvironmentName() + "-" + applicationEnvironment.getApplicationName() + "-Cognito-" + parameterName;
  }

  public CognitoOutputParameters getOutputParameters() {
    return new CognitoOutputParameters(
      this.userPool.getUserPoolId(),
      this.userPoolClient.getUserPoolClientId(),
      userPoolClientSecret,
      this.logoutUrl,
      this.userPool.getUserPoolProviderUrl());
  }

  public static CognitoOutputParameters getOutputParametersFromParameterStore(Construct scope, ApplicationEnvironment applicationEnvironment) {
    return new CognitoOutputParameters(
      getParameterUserPoolId(scope, applicationEnvironment),
      getParameterUserPoolClientId(scope, applicationEnvironment),
      getParameterUserPoolClientSecret(scope, applicationEnvironment),
      getParameterLogoutUrl(scope, applicationEnvironment),
      getParameterUserPoolProviderUrl(scope, applicationEnvironment));
  }

  private static String getParameterUserPoolId(Construct scope, ApplicationEnvironment applicationEnvironment) {
    return StringParameter.fromStringParameterName(scope, PARAMETER_USER_POOL_ID, createParameterName(applicationEnvironment, PARAMETER_USER_POOL_ID))
      .getStringValue();
  }

  private static String getParameterLogoutUrl(Construct scope, ApplicationEnvironment applicationEnvironment) {
    return StringParameter.fromStringParameterName(scope, PARAMETER_USER_POOL_LOGOUT_URL, createParameterName(applicationEnvironment, PARAMETER_USER_POOL_LOGOUT_URL))
      .getStringValue();
  }

  private static String getParameterUserPoolProviderUrl(Construct scope, ApplicationEnvironment applicationEnvironment) {
    return StringParameter.fromStringParameterName(scope, PARAMETER_USER_POOL_PROVIDER_URL, createParameterName(applicationEnvironment, PARAMETER_USER_POOL_PROVIDER_URL))
      .getStringValue();
  }

  private static String getParameterUserPoolClientId(Construct scope, ApplicationEnvironment applicationEnvironment) {
    return StringParameter.fromStringParameterName(scope, PARAMETER_USER_POOL_CLIENT_ID, createParameterName(applicationEnvironment, PARAMETER_USER_POOL_CLIENT_ID))
      .getStringValue();
  }

  private static String getParameterUserPoolClientSecret(Construct scope, ApplicationEnvironment applicationEnvironment) {
    return StringParameter.fromStringParameterName(scope, PARAMETER_USER_POOL_CLIENT_SECRET, createParameterName(applicationEnvironment, PARAMETER_USER_POOL_CLIENT_SECRET))
      .getStringValue();
  }

  public static class CognitoInputParameters {
    private final String applicationName;
    private final String applicationUrl;
    private final String loginPageDomainPrefix;

    public CognitoInputParameters(String applicationName, String applicationUrl, String loginPageDomainPrefix) {
      this.applicationName = applicationName;
      this.applicationUrl = applicationUrl;
      this.loginPageDomainPrefix = loginPageDomainPrefix;
    }
  }

  public static class CognitoOutputParameters {
    private final String userPoolId;
    private final String userPoolClientId;
    private final String userPoolClientSecret;
    private final String logoutUrl;
    private final String providerUrl;

    public CognitoOutputParameters(
      String userPoolId,
      String userPoolClientId,
      String userPoolClientSecret,
      String logoutUrl,
      String providerUrl) {
      this.userPoolId = userPoolId;
      this.userPoolClientId = userPoolClientId;
      this.userPoolClientSecret = userPoolClientSecret;
      this.logoutUrl = logoutUrl;
      this.providerUrl = providerUrl;
    }

    public String getUserPoolId() {
      return userPoolId;
    }

    public String getUserPoolClientId() {
      return userPoolClientId;
    }

    public String getUserPoolClientSecret() {
      return userPoolClientSecret;
    }

    public String getLogoutUrl() {
      return logoutUrl;
    }

    public String getProviderUrl() {
      return providerUrl;
    }
  }
}
