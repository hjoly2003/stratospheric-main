package dev.stratospheric.todoapp.cdk;

import java.util.Map;

import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.eventsources.SqsEventSource;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.sqs.IQueue;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

import static java.util.Collections.singletonList;

/**
 * [N]:chap7:jobqueue
 * @deprecated
 */
class DeploymentSequencerStack extends Stack {

	private final IQueue deploymentsQueue;
	private final LambdaFunction deploymentsLambda;

	/**
	 * [N]:chap7]:jobqueue - Used to deploy the Lambda and the SQS queue. 
	 * @param scope Parent of this stack, usually an `App` or a `Stage`, but could be any construct.
	 * @param id The construct ID of this stack.
	 * @param awsEnvironment The AWS environment (account/region) where this stack will be deployed.
	 * @param applicationName
	 * @param githubToken
	 */
	public DeploymentSequencerStack(
		final Construct scope,
		final String id,
		final Environment awsEnvironment,
		final String applicationName,
		final String githubToken) {
		super(scope, id, StackProps.builder()
			.stackName(applicationName + "-Deployments")
			.env(awsEnvironment).build());

		// [N] We create an SQS queue with fifo set to true. Note that the name of a FIFO queue always needs to have the suffix “.fifo”, otherwise there will be an error during deployment.
		this.deploymentsQueue = Queue.Builder.create(this, "deploymentsQueue")
			.queueName(applicationName + "-deploymentsQueue.fifo")
			.fifo(true)
			.build();

		SqsEventSource eventSource = SqsEventSource.Builder.create(deploymentsQueue)
			.build();

		// [N] Creates a LambdaFunction with the code from our Lambda project. The code of the Lambda is packaged within the lambda.zip archive in the deployment-sequencer-lambda/dist folder.
		this.deploymentsLambda = LambdaFunction.Builder.create(new Function(
			this,
			"deploymentSequencerFunction",
			FunctionProps.builder()
				// [N] We have to make sure to build a new .zip archive from the index.ts each time before we’re deploying the DeploymentSequencerStack.
				.code(Code.fromAsset("./deployment-sequencer-lambda/dist/lambda.zip"))
				// For the runtime, we choose Node 12 
				.runtime(Runtime.NODEJS_12_X)
				// [N] For the handler we provide index.handler because the handler function is called handler and it’s located in the file index.ts
				.handler("index.handler")
        		.logRetention(RetentionDays.TWO_WEEKS)
				// [N] We want to have a single instance that controls the sequencing of the deployment events. 
				.reservedConcurrentExecutions(1)
				// [N] We add an SqsEventSource to the Lambda.  This connects the SQS queue with the Lambda. Every event in the queue will now trigger the Lambda function.
				.events(singletonList(eventSource))
				// [N] We add some environment variables that the Lambda code needs to work.
				.environment(Map.of(
					"GITHUB_TOKEN", githubToken,
					"QUEUE_URL", deploymentsQueue.getQueueUrl(),
					"REGION", awsEnvironment.getRegion()
				)).build()
		)).build();

	}

}
