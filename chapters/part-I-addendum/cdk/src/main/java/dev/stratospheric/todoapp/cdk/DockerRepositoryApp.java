package dev.stratospheric.todoapp.cdk;

import dev.stratospheric.cdk.DockerRepository;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

/**
 * [N] This stack will only deploy a single CloudFormation resource, namely an ECR repository.
 */
public class DockerRepositoryApp {

  /**
   * @param args Must contain the applicationName, the AWS accountID and region.
   */
  public static void main(final String[] args) {

    App app = new App();

    /* 
     * [N] We can pass parameters into a CDK app with the -c command-line parameter or by adding them to the context section in the cdk.json file. 
     */
    String accountId = (String) app.getNode().tryGetContext("accountId");
    Validations.requireNonEmpty(accountId, "context variable 'accountId' must not be null");

    String region = (String) app.getNode().tryGetContext("region");
    Validations.requireNonEmpty(region, "context variable 'region' must not be null");

    /* [N] The name of the application for which we want to create a Docker repository. */
    String applicationName = (String) app.getNode().tryGetContext("applicationName");
    Validations.requireNonEmpty(applicationName, "context variable 'applicationName' must not be null");

    Environment awsEnvironment = makeEnv(accountId, region);

    // [N] We’re creating exactly one stack. We’re using the applicationName to prefix the name of the stack, so we can identify the stack quickly in CloudFormation.
    Stack dockerRepositoryStack = new Stack(app, "DockerRepositoryStack", StackProps.builder()
      .stackName(applicationName + "-DockerRepository")
      // [N] We pass this Environment object into the stack we create via the env() method on the builder.
      .env(awsEnvironment)
      .build()
    );

    /*
     * DockerRepository is another of the constructs from our constructs library (https://github.com/stratospheric-dev/cdk-constructs/)
     */
    DockerRepository dockerRepository = new DockerRepository(
      // [N] We’re passing in the dockerRepositoryStack as the scope argument, so that the construct will be added to that stack.
      dockerRepositoryStack,
      "DockerRepository",
      awsEnvironment,
      // [N] DockerRepositoryInputParameters as a parameter, which bundles all input parameters the construct needs into a single object. 
      new DockerRepository.DockerRepositoryInputParameters(applicationName, accountId)
    );

    app.synth();
  }

  static Environment makeEnv(String account, String region) {
    return Environment.builder()
      .account(account)
      .region(region)
      .build();
  }

}
