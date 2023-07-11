# Stratospheric - Chapter 9-10: Local Development, Building User Registration and Login with AWS Cognito

## Summary

### Chapter 9

]:local - We set the context to enable the Todo application to be deployed and run
locally.

]:localstack - We'll use LocalStack to emulate locally most of the AWS services. Its deployment configuration is specified with the `dev` profile (`application_dev.yml`).

]:local]:init_script - After booting up LocalStack, we need to create our resources (an S3 bucket or SQS queue). For this purpose, the LocalStack Docker container executes any shell script that is part of its `/docker-entrypoint-initaws.d` folder once all services are available. In fact, the init script (local-aws-infrastructure.sh) under the `docker-entrypoint-initaws.d` folder resides in the docker container and is a mapping of [./src/test/resources/localstack/local-aws-infrastructure.sh](./src/test/resources/localstack/local-aws-infrastructure.sh) on your local PC.

]:keycloak - Since Cognito is not supported by LocalStack, we’re using Keycloak as a compatible OIDC identity provider instead.

### Chapter 10

]:security]:cognito - The sample application needs to partition the todos by their respective owners. We will use Spring Security in conjunction with Amazon Cognito to achieve this and to perform both user authentication and authorization. Users will be identified by their email addresses. The user data itself will be stored in a Cognito user pool. Our application will use OIDC (OpenID Connect, an authentication framework on top of OAuth2) for retrieving and maintaining the user’s Cognito session.

* ]:usr-signup - To avoid bots from auto-generating user accounts, we add a layer of protection and require an invitation code on each signup. To that end, let’s create a public Thymeleaf view that contains the relevant information for new sign-ups.
* ]:web-filter - Although all of the application's endpoints are protected by Spring Security auto-configuration, our Todo application should have both a protected and public area (for landing page, user registration and basic web resources such as css, javascript).
* ]:logout - We need a logout process for each of our users to log out of both the Identity Provider session and of the  Spring web session.

### (in later chapters?)

]:ses]:sqs - The sample application allows users to share their todos with others through email notifications sent via Amazon Simple Email Service (SES) and Amazon Simple Queue Service (SQS).

]:mq - The sample application uses WebSockets and a managed Apache ActiveMQ message broker running on Amazon MQ to notify a todo owner right in the browser once a collaboration request for a todo has been accepted.

### Configuration

The `dev.stratospheric.config` package contains multiple configuration classes.

The `cloudformation` folder contains some CloudFormation templates for the AWS infrastructure
that we may use as an alternative to the CDK apps.

### Features

The application follows a package-by-feature structure. Hence, the feature folders contain the code artifacts related to these respective features. These code artifacts encompass controllers, service interfaces (and their implementations), Spring Data JPA repositories, and data model classes.

#### Running the app

### Chapter 9

Build and deploy the application

```bash
cd stratospheric-main/application
gradle build
docker-compose up
gradle bootRun
```

Open your browser to `http://localhost:8080/`.

We can still log in to the Todo application by using one of the pre-defined users:
* `tom` with password `stratospheric`
* `bjoern` with password `stratospheric`
* `philip` with password `stratospheric`

### Chapter 10 (Deploying to AWS)

To bootstrap our AWS environment

```bash
cdk bootstrap aws://<ACCOUNT_ID>/<REGION>
```

Since we're building the application on an arm64 platform, we need to create a Docker image compatible to the default AWS ECS architecture

```bash
cd chapter-10/application
gradle build
# The following requires Docker to be running: it builds the image in x86_64 and push it to DockerHub
docker buildx build --platform=linux/amd64 -t hjolydocker/todo-app-v1:latest --push .
```

#### Deploying the Generated CDK App:

```bash
cd chapter-10/cdk
npm run network:deploy
npm run cognito:deploy
npm run service:deploy
```

Then, have a look around in the AWS Console to see the resources those commands created.
Don't forget to delete the stacks afterward:

```bash
npm run service:destroy
npm run cognito:destroy
npm run network:destroy
```

#### To delete the CDKToolkit stack:

1. open its "Resources" tab from the CloudFormation.
2. Within the resource tab, copy the "Physical ID" of the AWS::S3::Bucket.
3. Within the S3 Management Console, select that same bucket and click the "Empty" button.
4. For that bucket click the "Delete" button.
5. Back in the CDKToolkit stack within the CloudFormation window, click the "Delete" button.
