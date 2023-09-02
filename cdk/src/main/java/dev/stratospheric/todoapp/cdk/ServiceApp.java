package dev.stratospheric.todoapp.cdk;

import dev.stratospheric.cdk.ApplicationEnvironment;
import dev.stratospheric.cdk.Network;
import dev.stratospheric.cdk.PostgresDatabase;
import dev.stratospheric.cdk.Service;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.secretsmanager.ISecret;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.constructs.Construct;

import static dev.stratospheric.todoapp.cdk.Validations.requireNonEmpty;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;

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

    // [N]:jpa - We load the database output parameters (SPRING_DATASOURCE_{URL|USERNAME|PASSWORD}) to make them available to Spring Boot. This will enable the application to access the database.
    PostgresDatabase.DatabaseOutputParameters databaseOutputParameters =
      PostgresDatabase.getOutputParametersFromParameterStore(parametersStack, applicationEnvironment);

    CognitoStack.CognitoOutputParameters cognitoOutputParameters =
      CognitoStack.getOutputParametersFromParameterStore(parametersStack, applicationEnvironment);

    MessagingStack.MessagingOutputParameters messagingOutputParameters =
      MessagingStack.getOutputParametersFromParameterStore(parametersStack, applicationEnvironment);

    ActiveMqStack.ActiveMqOutputParameters activeMqOutputParameters =
      ActiveMqStack.getOutputParametersFromParameterStore(parametersStack, applicationEnvironment);

    // [N] Provides the list of groups from which ingress is allowed.
    List<String> securityGroupIdsToGrantIngressFromEcs = Arrays.asList(
      databaseOutputParameters.getDatabaseSecurityGroupId(),
      // [N]:mq - To configure our application to access the MQ broker we need to add the message broker’s security group to the list of groups from which ingress is allowed.
      activeMqOutputParameters.getActiveMqSecurityGroupId()
    );

    new Service(
      serviceStack,
      "Service",
      awsEnvironment,
      applicationEnvironment,
      new Service.ServiceInputParameters(
        new Service.DockerImageSource(dockerRepositoryName, dockerImageTag),
        securityGroupIdsToGrantIngressFromEcs,
        environmentVariables(
          serviceStack,
          databaseOutputParameters,
          cognitoOutputParameters,
          messagingOutputParameters,
          activeMqOutputParameters,
          springProfile,
          environmentName))
        .withCpu(512)
        .withMemory(1024)

        // [N]:security - The Service construct of our cdk-constructs library takes a list of PolicyStatement objects, which are needed for configuring the access to internal AWS resources for our application.
        .withTaskRolePolicyStatements(List.of(
          // [N]:sqs - The following gives our application the necessary permissions to send, receive, and delete messages for any Amazon SQS queue as we’ve used the * wildcard.
          PolicyStatement.Builder.create()
            .sid("AllowSQSAccess")
            .effect(Effect.ALLOW)
            .resources(List.of(
              String.format("arn:aws:sqs:%s:%s:%s", region, accountId, messagingOutputParameters.getTodoSharingQueueName())
            ))
            .actions(Arrays.asList(
              "sqs:DeleteMessage",
              "sqs:GetQueueUrl",
              "sqs:ListDeadLetterSourceQueues",
              "sqs:ListQueues",
              "sqs:ListQueueTags",
              "sqs:ReceiveMessage",
              "sqs:SendMessage",
              "sqs:ChangeMessageVisibility",
              "sqs:GetQueueAttributes"))
            .build(),
            
          // [N]:cognito - The following policy allows all operations (like creating a user) on cognito-idp (IdP: identity Provider)
          PolicyStatement.Builder.create()
            .sid("AllowCreatingUsers")
            .effect(Effect.ALLOW)
            .resources(
              // [N] The cognito-identity prefix refers to Identity Pools.
              List.of(String.format("arn:aws:cognito-idp:%s:%s:userpool/%s", region, accountId, cognitoOutputParameters.getUserPoolId()))
            )
            .actions(List.of("cognito-idp:AdminCreateUser")).build(),

          // [N]:ses - the ECS task role requires sufficient permissions for the AWS SES API
          PolicyStatement.Builder.create()
            .sid("AllowSendingEmails")
            .effect(Effect.ALLOW)
            .resources(
              List.of(String.format("arn:aws:ses:%s:%s:identity/hjolystratos.net", region, accountId))
            )
            .actions(List.of("ses:SendEmail", "ses:SendRawEmail")).build(),

          // [N]:nosql - Add a new IAM PolicyStatement for the role assumed by our sample Todo application to be allowed to create new DynamoDB tables and items in those tables.
          PolicyStatement.Builder.create()
            .sid("AllowDynamoTableAccess")
            .effect(Effect.ALLOW)
            .resources(
              List.of(String.format("arn:aws:dynamodb:%s:%s:table/%s", region, accountId, applicationEnvironment.prefix("breadcrumb")))
            )
            .actions(List.of(
              "dynamodb:Scan",
              "dynamodb:Query",
              "dynamodb:PutItem",
              "dynamodb:GetItem",
              "dynamodb:BatchWriteItem",
              "dynamodb:BatchWriteGet"
              ))
            .build(),

          PolicyStatement.Builder.create()
            .sid("AllowSendingMetricsToCloudWatch")
            .effect(Effect.ALLOW)
            .resources(singletonList("*")) // CloudWatch does not have any resource-level permissions, see https://stackoverflow.com/a/38055068/9085273
            .actions(singletonList("cloudwatch:PutMetricData"))
            .build()
        ))
        // [N]:security - Ensures that users are always routed to the same service instance they were assigned to on their first request. Otherwize, having many instances of the application running in parallel exposes us to the risk of a session validation failure  or user authentication failure as these are node dependent (see "Shortcomings when Scaling Out" from Stratospheric chapter 10).
        .withStickySessionsEnabled(true)
        
        .withHealthCheckPath("/actuator/health")
        
        // [N]:logs - Defines the awslogs-datetime-format to be used by the "Service" stack defined in the stratospheric.cdk library (see https://github.com/stratospheric-dev/stratospheric/blob/main/cdk/src/main/java/dev/stratospheric/todoapp/cdk/ServiceApp.java).
        .withAwsLogsDateTimeFormat("%Y-%m-%dT%H:%M:%S.%f%z")

        .withHealthCheckIntervalSeconds(30), // needs to be long enough to allow for slow start up with low-end computing instances

      Network.getOutputParametersFromParameterStore(serviceStack, applicationEnvironment.getEnvironmentName()));

    app.synth();
  }

  /**
   * [N] Environment variables to be passed to the service stack.<p/>
   * Eventually the service stack will make them available to the Spring context of the application.
   * @param scope
   * @param databaseOutputParameters [N]:rds
   * @param cognitoOutputParameters [N]:security]:cognito
   * @param messagingOutputParameters [N]:sqs
   * @param activeMqOutputParameters [N]:mq - Output parameeters of the {@code ActiveMqStack}
   * @param springProfile
   * @param environmentName
   * @return [N]:jpa A map of all environment variables that should be injected into the Docker container of our Spring Boot app.
   * @see dev.stratospheric.cdk.Service The stratospheric cdk Service constructor line 178.
   */
  static Map<String, String> environmentVariables(
    Construct scope,
    PostgresDatabase.DatabaseOutputParameters databaseOutputParameters,
    CognitoStack.CognitoOutputParameters cognitoOutputParameters,
    MessagingStack.MessagingOutputParameters messagingOutputParameters,
    ActiveMqStack.ActiveMqOutputParameters activeMqOutputParameters,
    String springProfile,
    String environmentName
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

    // [N]:security]:cognito - Makes Cognito secure parameters available to the application's Spring security initialization. For the sake of simplicity, we're passing them as plain-text parameters instead of encrypted values.
    vars.put("COGNITO_CLIENT_ID", cognitoOutputParameters.getUserPoolClientId());
    vars.put("COGNITO_CLIENT_SECRET", cognitoOutputParameters.getUserPoolClientSecret());
    vars.put("COGNITO_USER_POOL_ID", cognitoOutputParameters.getUserPoolId());
    vars.put("COGNITO_LOGOUT_URL", cognitoOutputParameters.getLogoutUrl());
    vars.put("COGNITO_PROVIDER_URL", cognitoOutputParameters.getProviderUrl());

    vars.put("TODO_SHARING_QUEUE_NAME", messagingOutputParameters.getTodoSharingQueueName());

    // [N]:mq - Adds some of the output parameters of the ActiveMqStack as environment variables.
    vars.put("WEB_SOCKET_RELAY_ENDPOINT", activeMqOutputParameters.getStompEndpoint());
    vars.put("WEB_SOCKET_RELAY_USERNAME", activeMqOutputParameters.getActiveMqUsername());
    vars.put("WEB_SOCKET_RELAY_PASSWORD", activeMqOutputParameters.getActiveMqPassword());

    vars.put("ENVIRONMENT_NAME", environmentName);

    return vars;
  }

  static Environment makeEnv(String account, String region) {
    return Environment.builder()
      .account(account)
      .region(region)
      .build();
  }
}
