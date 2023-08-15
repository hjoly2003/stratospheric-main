package dev.stratospheric.todoapp.cdk;

import java.util.HashMap;
import java.util.Map;

import dev.stratospheric.cdk.ApplicationEnvironment;
import dev.stratospheric.cdk.Network;
import dev.stratospheric.cdk.Service;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

import static dev.stratospheric.todoapp.cdk.Validations.requireNonEmpty;

public class ServiceApp {

  /**
   * @param args Must contain the environmentName ("staging" or "prod"), the applicationName, the springProfile ("staging" or "prod"), the dockerImageUrl, the AWS accountID and region.
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

    Stack serviceStack = new Stack(app, "ServiceStack", StackProps.builder()
      .stackName(applicationEnvironment.prefix("Service"))
      .env(awsEnvironment)
      .build());

    // The Service construct exposes the class DockerImageSource, which allows us to specify the source of the Docker image that we want to deploy.
    Service.DockerImageSource dockerImageSource = new Service.DockerImageSource(dockerImageUrl);
    // [N] We’re also using the applicationEnvironment for accessing the output parameters of a previously deployed Network construct.
    Network.NetworkOutputParameters networkOutputParameters = Network.getOutputParametersFromParameterStore(serviceStack, applicationEnvironment.getEnvironmentName());
    Service.ServiceInputParameters serviceInputParameters = new Service.ServiceInputParameters(dockerImageSource, environmentVariables(springProfile))
      .withHealthCheckIntervalSeconds(30);

    // We then pass these parameters into the Service construct so that it can use them to bind the resources it deploys to the existing network infrastructure.
    Service service = new Service(
      serviceStack,
      "Service",
      awsEnvironment,
      applicationEnvironment,
      serviceInputParameters,
      networkOutputParameters);

    app.synth();
  }

  /**
   * The Service construct allows us to pass in a map with environment variables.
In our case, we’re adding the SPRING_PROFILES_ACTIVE variable with the value
of the springProfile variable
   * @param springProfile ("staging" or "prod")
   * @return
   */
  static Map<String, String> environmentVariables(String springProfile) {
    Map<String, String> vars = new HashMap<>();
    vars.put("SPRING_PROFILES_ACTIVE", springProfile);
    return vars;
  }

  static Environment makeEnv(String account, String region) {
    return Environment.builder()
      .account(account)
      .region(region)
      .build();
  }
}
