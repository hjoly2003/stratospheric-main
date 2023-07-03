package dev.stratospheric.todoapp.cdk;

import dev.stratospheric.cdk.ApplicationEnvironment;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;

import static dev.stratospheric.todoapp.cdk.Validations.requireNonEmpty;

/**
 * [N]:cognito - Used for the deployment of an AWS Cognito service.
 * @param applicationUrl This parameter allows us to pass the final base URL of our application. An example URL would be https://app.hjolystratos.net.
 * @param loginPageDomainPrefix Each user pool provides a customizable web UI for users to log in. We can either provide a custom domain or pass a prefix to use an Amazon Cognito domain that will look something like this: https://{prefix}.auth.{region}.amazoncognito.com.
 */
public class CognitoApp {

  public static void main(final String[] args) {
    App app = new App();

    String environmentName = (String) app.getNode().tryGetContext("environmentName");
    requireNonEmpty(environmentName, "context variable 'environmentName' must not be null");

    String applicationName = (String) app.getNode().tryGetContext("applicationName");
    requireNonEmpty(applicationName, "context variable 'applicationName' must not be null");

    String accountId = (String) app.getNode().tryGetContext("accountId");
    requireNonEmpty(accountId, "context variable 'accountId' must not be null");

    String region = (String) app.getNode().tryGetContext("region");
    requireNonEmpty(region, "context variable 'region' must not be null");

    String applicationUrl = (String) app.getNode().tryGetContext("applicationUrl");
    requireNonEmpty(applicationUrl, "context variable 'applicationUrl' must not be null");

    String loginPageDomainPrefix = (String) app.getNode().tryGetContext("loginPageDomainPrefix");
    requireNonEmpty(loginPageDomainPrefix, "context variable 'loginPageDomainPrefix' must not be null");

    Environment awsEnvironment = makeEnv(accountId, region);

    ApplicationEnvironment applicationEnvironment = new ApplicationEnvironment(
      applicationName,
      environmentName
    );

    new CognitoStack(app, "cognito", awsEnvironment, applicationEnvironment, new CognitoStack.CognitoInputParameters(
      applicationName,
      applicationUrl,
      loginPageDomainPrefix));

    app.synth();
  }

  static Environment makeEnv(String account, String region) {
    return Environment.builder()
      .account(account)
      .region(region)
      .build();
  }

}
