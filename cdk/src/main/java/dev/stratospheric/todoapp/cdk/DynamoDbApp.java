package dev.stratospheric.todoapp.cdk;

import dev.stratospheric.cdk.ApplicationEnvironment;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

/**
 * [N]:nosql - Implements a DynamoDB for tracing the user’s journey through our application.
 */
public class DynamoDbApp {

  public static void main(final String[] args) {
    App app = new App();

    String environmentName = (String) app.getNode().tryGetContext("environmentName");
    Validations.requireNonEmpty(environmentName, "context variable 'environmentName' must not be null");

    String applicationName = (String) app.getNode().tryGetContext("applicationName");
    Validations.requireNonEmpty(applicationName, "context variable 'applicationName' must not be null");

    String accountId = (String) app.getNode().tryGetContext("accountId");
    Validations.requireNonEmpty(accountId, "context variable 'accountId' must not be null");

    String region = (String) app.getNode().tryGetContext("region");
    Validations.requireNonEmpty(region, "context variable 'region' must not be null");

    Environment awsEnvironment = makeEnv(accountId, region);

    ApplicationEnvironment applicationEnvironment = new ApplicationEnvironment(
      applicationName,
      environmentName
    );

    Stack dynamoDbStack = new Stack(app, "DynamoDbStack", StackProps.builder()
      .stackName(applicationEnvironment.prefix("DynamoDb"))
      .env(awsEnvironment)
      .build());

    // [N]:web-trace - This breadcrumb table is created by the BreadcrumbsDynamoDbTable construct.
    new BreadcrumbsDynamoDbTable(
      dynamoDbStack,
      "BreadcrumbTable",
      applicationEnvironment,
      new BreadcrumbsDynamoDbTable.InputParameter("breadcrumb")
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
