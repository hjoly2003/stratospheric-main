# Stratospheric - Chapter 12: Sharing Todos with SQS and SES

## Status

* When building the application, we have to skip the unit tests since there's a unit test that fails:
  ```bash
  gradle -x test build
  ```
* Cannot deploy the service stack. We get the following from the log of the ECS:
  ```bash
  ~[app.jar:0.0.1-SNAPSHOT] Caused by: java.util.concurrent.CompletionException: io.awspring.cloud.sqs.QueueAttributesResolvingException: Error resolving attributes for queue stratospheric-todo-sharing with strategy CREATE and queueAttributesNames [] at ...
  ```

* There is a bug that hinder the login process (both in local and AWS deployment):
  1. When submitting the login form, you'll fall into a blank page with an http 999 error code.
  2. Click the back button from your browser to get to the previous page. You'll get to a page titled "Login with OAuth 2.0 [authorization_request_not_found]".
  3. In that page, click the bottom link to complete the login process and get to the targetted "index" page.

## Summary

We’ll implement the next feature for our Todo application and integrate two new AWS services:

* ]:share]:sqs]:listener - SQS (Simple Queue Service) to enable users to share their todo with any user of our application except themselves and only if they are the todo owner.
* ]:share]:ses - The SES (Simple Email Service) is used to inform the invited collaborator via email. For demonstration purposes and to avoid bootstrapping a new service, we’ll self-consume the messages in the same application. 

]:receiver - The user a todo has been shared with gets a notification and has to accept the collaboration first.

Whenever a user decides to share one of their todos, we’ll first store this request in an SQS queue. The queue acts as a buffer, and another part of our application will then handle the incoming requests and send out emails.

]:local - We update the LocalStack to include SQS and SES.

## Running the app locally

Start Docker.

Build and deploy the application

```bash
cd chapter-12/application
gradle build
docker-compose up
gradle bootRun
```

Open your browser to `http://localhost:8080/`.

## Running the app on AWS

To bootstrap our AWS environment

```bash
cdk bootstrap aws://<ACCOUNT_ID>/<REGION>
```

We first need to bootstrap the ECR so that, later, it will store our application image.

```bash
cd chapter-12/cdk
npm run repository:deploy
```

Start docker and then the following commands:

```bash
cd chapter-12/application
gradle -x test build
# The following requires AWS_REGION and ACCCOUNT_ID to be defined in the shell session.
aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com
# The following requires Docker to be running: it builds the image in amd64 and push it to the ECR
docker buildx build --platform=linux/amd64 -t ${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/todo-app:DYNAMICALLY_OVERRIDDEN_BY_WORKFLOW --push .
```

#### Deploying the Generated CDK App:

```bash
cd chapter-12/cdk
npm run network:deploy
npm run cognito:deploy
npm run database:deploy
npm run messaging:deploy
npm run service:deploy # Note: it might happen that the deployment takes forever. To troubleshoot, on the console, go to ECS > Cluster > staging-todo-app. Pick the "Events" tab & check the messages for something wrong. In that message, click the unsuccessfull task. Most often its a wrong image name
npm run domain:deploy

# Can also run everything as a batch job and get notified when done
npm run network:deploy; npm run cognito:deploy; npm run database:deploy; npm run messaging:deploy; npm run service:deploy; npm run domain:deploy; Say "complété"
```

The toto application is accessible at the following URL: https://app.hjolystratos.net.

#### Destroying the Stacks

To conclude, have a look around in the AWS Console to see the resources those commands created.
Don't forget to delete the stacks afterward:

```bash
npm run domain:destroy
npm run service:destroy
npm run messaging:destroy
npm run database:destroy  # [!] Prior to this, we might need to clean up a DB schema from the DB. 

npm run cognito:destroy # [!] Note that, prior to this, you have to go to the AWS console to delete the User Pool.
npm run network:destroy
npm run repository:destroy # [!] Note that, prior to this, you have to go to the AWS console to delete the Docker image.

# Can also run everything as a batch job and get notified when done
npm run domain:destroy; npm run service:destroy; npm run messaging:destroy; npm run database:destroy; npm run cognito:destroy; npm run network:destroy; npm run repository:destroy; Say "complété"
```

#### To delete the CDKToolkit stack:

1. open its "Resources" tab from the CloudFormation.
2. Within the resource tab, copy the "Physical ID" of the AWS::S3::Bucket.
3. Within the S3 Management Console, select that same bucket and click the "Empty" button.
4. For that bucket click the "Delete" button.
5. Back in the CDKToolkit stack within the CloudFormation window, click the "Delete" button.
