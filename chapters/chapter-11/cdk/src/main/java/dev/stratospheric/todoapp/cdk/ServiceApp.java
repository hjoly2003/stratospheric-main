package dev.stratospheric.todoapp.cdk;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import dev.stratospheric.cdk.ApplicationEnvironment;
import dev.stratospheric.cdk.Network;
import dev.stratospheric.cdk.PostgresDatabase;
import dev.stratospheric.cdk.Service;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.secretsmanager.ISecret;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.constructs.Construct;

import static dev.stratospheric.todoapp.cdk.Validations.requireNonEmpty;

public class ServiceApp {

  /**
   * @param args Must contain the environmentName ("staging" or "prod"), the applicationName, the AWS accountID, the springProfile ("staging" or "prod"), the dockerRepositoryName, the dockerImageTag and region.
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

    String dockerRepositoryName = (String) app.getNode().tryGetContext("dockerRepositoryName");
    requireNonEmpty(dockerRepositoryName, "context variable 'dockerRepositoryName' must not be null");

    String dockerImageTag = (String) app.getNode().tryGetContext("dockerImageTag");
    requireNonEmpty(dockerImageTag, "context variable 'dockerImageTag' must not be null");

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
    long timestamp = System.currentTimeMillis();
    Stack parametersStack = new Stack(app, "ServiceParameters-" + timestamp, StackProps.builder()
      .stackName(applicationEnvironment.prefix("Service-Parameters-" + timestamp))
      .env(awsEnvironment)
      .build());

    Stack serviceStack = new Stack(app, "ServiceStack", StackProps.builder()
      .stackName(applicationEnvironment.prefix("Service"))
      .env(awsEnvironment)
      .build());

    // [N]:jpa - We load the database output parameters (SPRING_DATASOURCE_{URL|USERNAME|PASSWORD}) to make them available to Spring Boot. 
    PostgresDatabase.DatabaseOutputParameters databaseOutputParameters =
      PostgresDatabase.getOutputParametersFromParameterStore(parametersStack, applicationEnvironment);

    new Service(
      serviceStack,
      "Service",
      awsEnvironment,
      applicationEnvironment,
      new Service.ServiceInputParameters(
        new Service.DockerImageSource(dockerRepositoryName, dockerImageTag),
        Collections.singletonList(databaseOutputParameters.getDatabaseSecurityGroupId()),
        environmentVariables(
          serviceStack,
          databaseOutputParameters,
          springProfile))
        // [N]:security - Ensures that users are always routed to the same service instance they were assigned to on their first request. Otherwize, having many instances of the application running in parallel exposes us to the risk of a session validation failure  or user authentication failure as these are node dependent (see "Shortcomings when Scaling Out" from Stratospheric chapter 10).
        .withStickySessionsEnabled(true)
        .withHealthCheckIntervalSeconds(30), // needs to be long enough to allow for slow start up with low-end computing instances

      Network.getOutputParametersFromParameterStore(serviceStack, applicationEnvironment.getEnvironmentName()));

    app.synth();
  }

  /**
   * @param scope
   * @param databaseOutputParameters
   * @param cognitoOutputParameters
   * @param messagingOutputParameters
   * @param activeMqOutputParameters
   * @param springProfile
   * @param environmentName
   * @return [N]:jpa A map of all environment variables that should be injected into the Docker container of our Spring Boot app.
   */
  static Map<String, String> environmentVariables(
    Construct scope,
    PostgresDatabase.DatabaseOutputParameters databaseOutputParameters,
    String springProfile
  ) {
    Map<String, String> vars = new HashMap<>();

    String databaseSecretArn = databaseOutputParameters.getDatabaseSecretArn();
    ISecret databaseSecret = Secret.fromSecretCompleteArn(scope, "databaseSecret", databaseSecretArn);

    vars.put("SPRING_PROFILES_ACTIVE", springProfile);

    // [N]:jpa - We add the default environment variables Spring Boot uses for defining the database connection. We combine the parameters EndpointAddress, EndpointPort, and DBName to create a valid JDBC URL of this format: jdbc:postgresql://{EndpointAddress}:{EndpointPort}/{DBName}
    vars.put("SPRING_DATASOURCE_URL",
      String.format("jdbc:postgresql://%s:%s/%s",
        databaseOutputParameters.getEndpointAddress(),
        databaseOutputParameters.getEndpointPort(),
        databaseOutputParameters.getDbName()));

    // [N]:jpa - We load the username and password from the Secret we created in the database stack.
    vars.put("SPRING_DATASOURCE_USERNAME",
      databaseSecret.secretValueFromJson("username").toString());
    vars.put("SPRING_DATASOURCE_PASSWORD",
      databaseSecret.secretValueFromJson("password").toString());

    return vars;
  }

  static Environment makeEnv(String account, String region) {
    return Environment.builder()
      .account(account)
      .region(region)
      .build();
  }
}
