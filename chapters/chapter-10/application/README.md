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
./gradlew build
docker-compose up
./gradlew bootRun
```

Open your browser to `http://localhost:8080/`.
