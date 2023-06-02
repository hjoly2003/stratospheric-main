/* 
 * [N]:chap7]:jobqueue - Each time a deployment request arrives in an Amazon SQS queue, it will be processed by an AWS Lambda function. This Lambda function will then trigger a deployment only if there is currently no deployment running. The Lambda will trigger the “Deploy” workflow via the GitHub API, which finally uses our CDK app to deploy the service stack.
 * @deprecated Not used in the latest version of the code.
 */
import { GetQueueAttributesCommand, GetQueueAttributesResult, SQSClient, } from "@aws-sdk/client-sqs";
import axios from "axios";

export const handler = async (e: SqsEvent): Promise<any> => {
  const queueUrl = process.env.QUEUE_URL as string;
  const region = process.env.REGION as string;
  const githubToken = process.env.GITHUB_TOKEN as string;
  const event = new SqsEventWrapper(e);
  const latestDeploymentEvent: DeploymentEvent = event.getLatestDeploymentEvent();
  const github = new GitHub(githubToken);
  const queue = new DeploymentQueue(queueUrl, region);

  console.log(`Received event: ${JSON.stringify(latestDeploymentEvent)}`);

  // If there are more events in the queue, we just finish processing and wait for the next event.
  // [N]:chap7:]jobqueue - This is a FIFO queue: newest event in - oldest event out. Since the current event is the oldest, then the other newer one has priority and this one shall be skipped.
  if (await queue.hasWaitingEvents()) {
    console.log(
      "Skipping this event because there are more events waiting in the queue!"
    );
    return;
  }

  // [N] If the GitHub workflow is currently running (i.e. if the "Deploy" workflow is currently processing an event), we throw an error to retry this event at a later time.
  if (await github.isWorkflowCurrentlyRunning(latestDeploymentEvent)) {
    console.log(
      "GitHub workflow is currently running - retrying at a later time!"
    );
    // [N] If the Lambda invocation exits with an error it signals to the SQS queue that the event has not been processed successfully, and it will be tried again after some time.
    throw "retrying later!";
  }

  // Triggering the GitHub workflow.
  await github.triggerWorkflow(latestDeploymentEvent);
};

class GitHub {
  githubToken: string;

  constructor(githubToken: string) {
    this.githubToken = githubToken;
  }

  /**
   * Calls the GitHub API to check if the workflow defined by the given DeploymentEvent is currently running.
   */
  async isWorkflowCurrentlyRunning(event: DeploymentEvent): Promise<boolean> {
    console.log(
      `checking if GitHub workflow is running for event: ${JSON.stringify(
        event
      )}`
    );

    const response = await axios.get(
      `https://api.github.com/repos/${event.owner}/${event.repo}/actions/workflows/${event.workflowId}/runs`,
      this.axiosConfig()
    );

    // checking if the response contains at least one current workflow run that is not 'completed'
    // (see https://docs.github.com/en/rest/reference/actions#list-workflow-runs)
    const inProgressWorkflowRuns: WorkflowRun[] = response.data.workflow_runs.filter(
      (run: WorkflowRun) => {
        return run.status != "completed";
      }
    );

    return Promise.resolve(inProgressWorkflowRuns.length > 0);
  }

  /**
   * Calls the GitHub API to trigger a GitHub Actions workflow for the given DeploymentEvent.
   */
  async triggerWorkflow(event: DeploymentEvent) {
    console.log(
      `triggering GitHub workflow for event: ${JSON.stringify(event)}`
    );

    const requestData = {
      ref: event.ref,
      inputs: {
        "docker-image-tag": event.dockerImageTag,
      },
    };

    const response = await axios.post(
      `https://api.github.com/repos/${event.owner}/${event.repo}/actions/workflows/${event.workflowId}/dispatches`,
      requestData,
      this.axiosConfig()
    );
  }

  private axiosConfig() {
    return {
      headers: {
        Authorization: `token ${this.githubToken}`,
      },
    };
  }
}

interface WorkflowRun {
  status: String;
}

class DeploymentQueue {
  queueUrl: string;
  sqsClient: SQSClient;

  constructor(queueUrl: string, region: string) {
    this.queueUrl = queueUrl;
    this.sqsClient = new SQSClient({region: region});
  }

  async hasWaitingEvents(): Promise<boolean> {

    console.log(
      `checking queue ${this.queueUrl} for waiting events`
    );

    const params = {
      QueueUrl: this.queueUrl,
      AttributeNames: [
        "ApproximateNumberOfMessages",
        "ApproximateNumberOfMessagesDelayed",
        "ApproximateNumberOfMessagesNotVisible",
      ],
    };
    const command = new GetQueueAttributesCommand(params);
    const data: GetQueueAttributesResult = await this.sqsClient.send(command);

    if (data.Attributes == undefined) {
      throw "GetQueueAttributesResult has no attributes!";
    }

    console.log(`GetQueueAttributesResult: ${JSON.stringify(data)}`);

    const waitingMessages: number =
      parseInt(data.Attributes["ApproximateNumberOfMessages"]) +
      parseInt(data.Attributes["ApproximateNumberOfMessagesDelayed"]) +
      parseInt(data.Attributes["ApproximateNumberOfMessagesNotVisible"]) -
      1; // minus one because the message currently processed by this Lambda is counted as a "not visible" message

    return Promise.resolve(waitingMessages > 0);
  }
}

class SqsEventWrapper {
  event: SqsEvent;

  constructor(event: SqsEvent) {
    this.event = event;
  }

  isEmpty(): boolean {
    return this.numberOfDeploymentEvents() == 0;
  }

  /**
   * Returns the number of deployment events in this batch.
   */
  numberOfDeploymentEvents(): number {
    return this.event.Records.length;
  }

  /**
   * Returns the latest event in this batch of events.
   */
  getLatestDeploymentEvent(): DeploymentEvent {
    return JSON.parse(
      this.event.Records.sort((e1, e2) => {
        return (
          Number(e1.attributes.SequenceNumber) -
          Number(e2.attributes.SequenceNumber)
        );
      })
        .reverse()
        .shift()!.body
    );
  }
}

/* 
 * [?] An event (SqsEvent) that contains a list of events (Records)? Maybe SqsEvent.records is a list of one event most of the time.
 */
interface SqsEvent {
  Records: Record[];
}

interface Record {
  messageId: string;
  body: string;
  attributes: RecordAttributes;
}

interface RecordAttributes {
  SequenceNumber: string;
}

interface DeploymentEvent {
  commitSha: string;
  ref: string;
  owner: string;
  repo: string;
  workflowId: string;
  dockerImageTag: string;
}
