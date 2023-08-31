package dev.stratospheric.todoapp.cdk;

import dev.stratospheric.cdk.ApplicationEnvironment;
import dev.stratospheric.cdk.Network;
import dev.stratospheric.cdk.Service;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;

import static dev.stratospheric.todoapp.cdk.Validations.requireNonEmpty;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceApp {

  /**
   * @param args Must contain the environmentName ("staging" or "prod"), the applicationName, the AWS accountID, the springProfile ("staging" or "prod"), the dockerImageUrl and region.
   */
  public static void main(final String[] args) {
    App app = new App();

    // [N] Either "staging" or "production". The environmentName is used to be able to create multiple stacks for different environments from the same CDK app.
    String environmentName = (String) app.getNode().tryGetContext("environmentName");
    requireNonEmpty(environmentName, "context variable 'environmentName' must not be null");

    String applicationName = (String) app.getNode().tryGetContext("applicationName");
    requireNonEmpty(applicationName, "context variable 'applicationName' must not be null");

    // [N] AWS account ID asscociated to the IAM user account.
    String accountId = (String) app.getNode().tryGetContext("accountId");
    requireNonEmpty(accountId, "context variable 'accountId' must not be null");

    String springProfile = (String) app.getNode().tryGetContext("springProfile");
    requireNonEmpty(springProfile, "context variable 'springProfile' must not be null");

    String dockerImageUrl = (String) app.getNode().tryGetContext("dockerImageUrl");
    requireNonEmpty(dockerImageUrl, "context variable 'dockerImageUrl' must not be null");

    String region = (String) app.getNode().tryGetContext("region");
    requireNonEmpty(region, "context variable 'region' must not be null");

    Environment awsEnvironment = makeEnv(accountId, region);

    // [N] We use this ApplicationEnvironment object to prefix the name of the stack we’re creating. The Service construct also uses it internally to prefix the names of the resources it creates. So, given the environmentName “staging” and the applicationName “todoapp”, all resources will be prefixed with staging-todoapp- to account for the deployment of multiple Service stacks, each with a different application.
    ApplicationEnvironment applicationEnvironment = new ApplicationEnvironment(
      applicationName,
      environmentName
    );

    // This stack is just a container for the parameters below, because they need a Stack as a scope.
    // We're making this parameters stack unique with each deployment by adding a timestamp, because updating an existing
    // parameters stack will fail because the parameters may be used by an old service stack.
    // This means that each update will generate a new parameters stack that needs to be cleaned up manually!
    // [!]long timestamp = System.currentTimeMillis();
    String timestamp = "202308090709";
    Stack parametersStack = new Stack(app, "ServiceParameters-" + timestamp, StackProps.builder()
      .stackName(applicationEnvironment.prefix("Service-Parameters-" + timestamp))
      .env(awsEnvironment)
      .build());

    Stack serviceStack = new Stack(app, "ServiceStack", StackProps.builder()
      .stackName(applicationEnvironment.prefix("Service"))
      .env(awsEnvironment)
      .build());

    CognitoStack.CognitoOutputParameters cognitoOutputParameters =
      CognitoStack.getOutputParametersFromParameterStore(parametersStack, applicationEnvironment);


    new Service(
      serviceStack,
      "Service",
      awsEnvironment,
      applicationEnvironment,
      new Service.ServiceInputParameters(
        new Service.DockerImageSource(dockerImageUrl),
        Collections.emptyList(), 
        environmentVariables(
          springProfile, 
          cognitoOutputParameters))
        // [N]:security - The Service construct of our cdk-constructs library takes a list of PolicyStatement objects, which are needed for configuring the access to internal AWS resources for our application.
        .withTaskRolePolicyStatements(List.of(
          // [N]:cognito - The following policy allows all operations (like creating a user) on cognito-idp (IdP: identity Provider)
          PolicyStatement.Builder.create()
            .sid("AllowCreatingUsers")
            .effect(Effect.ALLOW)
            .resources(
              // [N] The cognito-identity prefix refers to Identity Pools.
              List.of(String.format("arn:aws:cognito-idp:%s:%s:userpool/%s", region, accountId, cognitoOutputParameters.getUserPoolId()))
            )
            .actions(List.of("cognito-idp:AdminCreateUser")).build()
          )
        )
        // [N]:security - Ensures that users are always routed to the same service instance they were assigned to on their first request. Otherwize, having many instances of the application running in parallel exposes us to the risk of a session validation failure  or user authentication failure as these are node dependent (see "Shortcomings when Scaling Out" from Stratospheric chapter 10).
        .withStickySessionsEnabled(true)
        .withHealthCheckIntervalSeconds(30), // needs to be long enough to allow for slow start up with low-end computing instances
      Network.getOutputParametersFromParameterStore(serviceStack, applicationEnvironment.getEnvironmentName()));

    app.synth();
  }

  /**
   * @param springProfile
   * @param cognitoOutputParameters
   * @return [N]:jpa A map of all environment variables that should be injected into the Docker container of our Spring Boot app.
   */
  static Map<String, String> environmentVariables(String springProfile, CognitoStack.CognitoOutputParameters cognitoOutputParameters) {
    Map<String, String> vars = new HashMap<>();
    vars.put("SPRING_PROFILES_ACTIVE", springProfile);

    // [N]:security]:cognito - Makes Cognito secure parameters available to the application's Spring security initialization. For the sake of simplicity, we're passing them as plain-text parameters instead of encrypted values.
    vars.put("COGNITO_CLIENT_ID", cognitoOutputParameters.getUserPoolClientId());
    vars.put("COGNITO_CLIENT_SECRET", cognitoOutputParameters.getUserPoolClientSecret());
    vars.put("COGNITO_USER_POOL_ID", cognitoOutputParameters.getUserPoolId());
    vars.put("COGNITO_LOGOUT_URL", cognitoOutputParameters.getLogoutUrl());
    vars.put("COGNITO_PROVIDER_URL", cognitoOutputParameters.getProviderUrl());
    return vars;
  }

  static Environment makeEnv(String account, String region) {
    return Environment.builder()
      .account(account)
      .region(region)
      .build();
  }
}
