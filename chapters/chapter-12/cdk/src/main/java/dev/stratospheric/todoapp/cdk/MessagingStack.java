package dev.stratospheric.todoapp.cdk;

import dev.stratospheric.cdk.ApplicationEnvironment;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.amazon.awscdk.services.sqs.IQueue;
import software.amazon.awscdk.services.sqs.Queue;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

/**
 * [N]:sqs - Creates both the Amazon SQS processing and dead-letter queues (dlq).
 */
class MessagingStack extends Stack {

  private final ApplicationEnvironment applicationEnvironment;
  private final IQueue todoSharingQueue;
  private final IQueue todoSharingDlq;

  public MessagingStack(
    final Construct scope,
    final String id,
    final Environment awsEnvironment,
    final ApplicationEnvironment applicationEnvironment) {
    super(scope, id, StackProps.builder()
      .stackName(applicationEnvironment.prefix("Messaging"))
      .env(awsEnvironment).build());

    this.applicationEnvironment = applicationEnvironment;

    this.todoSharingDlq = Queue.Builder.create(this, "todoSharingDlq")
      .queueName(applicationEnvironment.prefix( "todo-sharing-dead-letter-queue"))
      .retentionPeriod(Duration.days(14))
      .build();

    this.todoSharingQueue = Queue.Builder.create(this, "todoSharingQueue")
      .queueName(applicationEnvironment.prefix("todo-sharing-queue"))
      // [N] Sets the visibilityTimeout to 30 seconds, which should give our Spring Boot application plenty of time to store the collaboration request in the database and send an email.
      .visibilityTimeout(Duration.seconds(30))
      .retentionPeriod(Duration.days(14))
      // [N] We connect the main processing queue with the dead-letter queue by passing the field todoSharingDlq to the DeadLetterQueue builder. 
      .deadLetterQueue(DeadLetterQueue.builder()
        .queue(todoSharingDlq)
        .maxReceiveCount(3)
        .build())
      .build();

    createOutputParameters();

    applicationEnvironment.tag(this);
  }

  private static final String PARAMETER_TODO_SHARING_QUEUE_NAME = "todoSharingQueueName";

  /**
   * [N] Exposes the name of the SQS queue our application will connect to.<p/>
   * [N] Stores the name of the queue in the SSM parameter store so we can later use it to configure our Spring Boot application. Later on, we can create an AWS CloudWatch alarm to get notified as soon as the first message arrives in this queue.
   */
  private void createOutputParameters() {

    StringParameter.Builder.create(this, "todoSharingQueueName")
      .parameterName(createParameterName(applicationEnvironment, PARAMETER_TODO_SHARING_QUEUE_NAME))
      .stringValue(this.todoSharingQueue.getQueueName())
      .build();

  }

  private static String createParameterName(ApplicationEnvironment applicationEnvironment, String parameterName) {
    return applicationEnvironment.getEnvironmentName() + "-" + applicationEnvironment.getApplicationName() + "-Messaging-" + parameterName;
  }

  public static String getTodoSharingQueueName(Construct scope, ApplicationEnvironment applicationEnvironment) {
    return StringParameter.fromStringParameterName(scope, PARAMETER_TODO_SHARING_QUEUE_NAME, createParameterName(applicationEnvironment, PARAMETER_TODO_SHARING_QUEUE_NAME))
      .getStringValue();
  }

  public static MessagingOutputParameters getOutputParametersFromParameterStore(Construct scope, ApplicationEnvironment applicationEnvironment) {
    return new MessagingOutputParameters(
      getTodoSharingQueueName(scope, applicationEnvironment)
    );
  }

  public static class MessagingOutputParameters {
    private final String todoSharingQueueName;

    public MessagingOutputParameters(String todoSharingQueueName) {
      this.todoSharingQueueName = todoSharingQueueName;
    }

    public String getTodoSharingQueueName() {
      return todoSharingQueueName;
    }
  }

}
