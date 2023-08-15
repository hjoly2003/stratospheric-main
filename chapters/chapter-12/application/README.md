# Stratospheric - Chapter 12: Sharing Todos with SQS and SES

We’ll implement the next feature for our Todo application and integrate two new AWS services:

* ]:share]:sqs - SQS (Simple Queue Service) to enable users to share their todo with any user of our application except themselves and only if they are the todo owner.
* ]:share]:ses - The SES (Simple Email Service) is used to inform the invited collaborator via email.

]:receiver - The user a todo has been shared with gets a notification and has to accept the collaboration first.

Whenever a user decides to share one of their todos, we’ll first store this request in an SQS queue. The queue acts as a buffer, and another part of our application will then handle the incoming requests and send out emails.

## Running the app

Run ``gradle bootRun`` from the command line.

Open your browser to `http://localhost:8080/`.
