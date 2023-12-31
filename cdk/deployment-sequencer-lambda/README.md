# Deployment Sequencer Lambda

> ⚠️ This feature was superseded by a native GitHub Actions feature. This repository is kept for reference. We do not recommend using this approach as this involves more code and moving parts to maintain. Furthermore, we do not monitor any API changes for this approach and the code may no longer work. Download a recent version of the Stratospheric ebook in your Leanpub library if you're still reading an ebook version that favors this approach.

This is the source for a Lambda function that makes sure that the latest deployment event will trigger a GitHub Actions workflow.

Events have this shape:

```json
{
  "Records": [
    {
      "messageId": "19dd0b57-b21e-4ac1-bd88-01bbb068cb78",
      "receiptHandle": "MessageReceiptHandle",
      "body": "{\"commitSha\": \"${GITHUB_SHA}\", \"ref\": \"master\", \"owner\": \"blogtrack\", \"repo\": \"blogtrack\", \"workflowId\": \"deploy-to-staging.yml\", \"dockerImageTag\": \"${DOCKER_IMAGE_TAG}\"}",
      "attributes": {
        "ApproximateReceiveCount": "1",
        "SentTimestamp": "1523232000000",
        "SenderId": "123456789012",
        "ApproximateFirstReceiveTimestamp": "1523232000001"
      },
      "messageAttributes": {},
      "md5OfBody": "{{{md5_of_body}}}",
      "eventSource": "aws:sqs",
      "eventSourceARN": "arn:aws:sqs:ap-southeast-2:123456789012:MyQueue",
      "awsRegion": "ap-southeast-2"
    }
  ]
}
```

# [!] Including the required node modules

Open a terminal and go to the `stratos-chapter-7/cdk/deployment-sequencer-lambda`.
Install the required node modules:

```bash
$ npm install axios --save
```

That should resolve the missing modules within `index.ts (see [Cannot find module &#39;axios&#39;](https://stackoverflow.com/a/59475765/21907969))
