# Todo Application for Stratospheric

The purpose of this todo application is to serve as an example for the various use cases covered by the book.

## Status

The unit tests do not pass all:
```bash
application% gradle build

> Task :test

DockerComposeEnvironmentVerificationTest > shouldStartApplicationWithDockerComposeEnvironment() FAILED
    java.lang.ExceptionInInitializerError at NativeConstructorAccessorImpl.java:-2
        Caused by: org.testcontainers.containers.ContainerLaunchException at DockerComposeEnvironmentVerificationTest.java:30
            Caused by: org.rnorth.ducttape.RetryCountExceededException at DockerComposeEnvironmentVerificationTest.java:30
                Caused by: org.testcontainers.containers.ContainerLaunchException at DockerComposeEnvironmentVerificationTest.java:30
                    Caused by: org.testcontainers.containers.ContainerLaunchException at DockerComposeEnvironmentVerificationTest.java:30
<==========---> 78% EXECUTING [17s]
> :test > 1 test completed, 1 failed
> :test > Executing test dev.stratospheric.todoapp.BasicWebTest
```

Can't deploy the canary on AWS:
```bash
cdk% npm run canary:deploy

staging-todo-app-Canary:  start: Building 955380410f14f07921a5240003979001cbb09e891539deec7e0262473e5dd056:254857894179-us-east-1
staging-todo-app-Canary:  success: Built 955380410f14f07921a5240003979001cbb09e891539deec7e0262473e5dd056:254857894179-us-east-1
staging-todo-app-Canary:  start: Publishing 955380410f14f07921a5240003979001cbb09e891539deec7e0262473e5dd056:254857894179-us-east-1
staging-todo-app-Canary:  success: Published 955380410f14f07921a5240003979001cbb09e891539deec7e0262473e5dd056:254857894179-us-east-1
canary (staging-todo-app-Canary): deploying... [1/1]
staging-todo-app-Canary: creating CloudFormation changeset...
14:57:52 | CREATE_FAILED        | AWS::S3::Bucket             | canaryBucket3FA6AB3F
staging-todo-app-canary-bucket already exists


 ❌  canary (staging-todo-app-Canary) failed: Error: The stack named staging-todo-app-Canary failed creation, it may need to be manually deleted from the AWS console: ROLLBACK_COMPLETE: staging-todo-app-canary-bucket already exists
    at FullCloudFormationDeployment.monitorDeployment (/opt/homebrew/lib/node_modules/aws-cdk/lib/index.js:412:10236)
    at process.processTicksAndRejections (node:internal/process/task_queues:95:5)
    at async Object.deployStack2 [as deployStack] (/opt/homebrew/lib/node_modules/aws-cdk/lib/index.js:415:153172)
    at async /opt/homebrew/lib/node_modules/aws-cdk/lib/index.js:415:136968

 ❌ Deployment failed: Error: The stack named staging-todo-app-Canary failed creation, it may need to be manually deleted from the AWS console: ROLLBACK_COMPLETE: staging-todo-app-canary-bucket already exists
    at FullCloudFormationDeployment.monitorDeployment (/opt/homebrew/lib/node_modules/aws-cdk/lib/index.js:412:10236)
    at process.processTicksAndRejections (node:internal/process/task_queues:95:5)
    at async Object.deployStack2 [as deployStack] (/opt/homebrew/lib/node_modules/aws-cdk/lib/index.js:415:153172)
    at async /opt/homebrew/lib/node_modules/aws-cdk/lib/index.js:415:136968

The stack named staging-todo-app-Canary failed creation, it may need to be manually deleted from the AWS console: ROLLBACK_COMPLETE: staging-todo-app-canary-bucket already exists
```


## Getting Started

### Chapter 13. Push Notifications with Amazon MQ
* ]:mq]:websocket]:stomp - We will be notifying users in their browser window when another user has accepted their request to collaborate on a todo. To do so, we’ll be using WebSocket and STOMP as protocols on top of an ActiveMQ message broker running on Amazon MQ175.
    * ]:relay - To not expose our ActiveMQ to the public internet and to control the message flow to some extent, we’ll use our Spring Boot application as a relay between our HTML frontend and ActiveMQ.

* ]:statefull]:mq - We want to run (at least) two instances of our Spring Boot application for reliability. In this setting, only clients connected to a specific instance would be able to exchange messages with each other. Hence, to communicate across instances we need a stateful service that enables message sharing across application instances. We’ll employ an Amazon MQ service using Apache ActiveMQ (as a message broker) for that stateful service.

* ]:mq]:local - For local development, we'll configure an ActiveMQ instance running locally, so we don’t have to connect to a remote instance.


### Chapter 14. Tracing User Actions with Amazon DynamoDB

* ]:nosql]:web-trace - We're tracing the user’s journey through our application. It'll show which links are clicked the most and which features are used most frequently.
    * ]:spring-evnt - Using [Spring Events](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#context-functionalityevents), we’ll generate this data by emitting events whenever a user executes a relevant action in our application. We’ll use an @EventListener to store this data in a DynamoDB table mapped to an entity in our application.

* ]:local - We add dynamodb as an additional service to our LocalStack environment configuration.


### Chapter 15. Structured Logging with Amazon CloudWatch

* ]:log - In this chapter, we focus on application logs, because they are the most important in day-to-day operations. We implement a logging solution with:
    * ]:structured - structured logs in JSON format and 
    * ]:custom-fields - custom fields for logging
The result will allow us to search and filter structured log events from our Todo app with Amazon CloudWatch - Amazon’s main observability service, providing log query and dashboard capabilities for log data.



### Prerequisites

* [Java 17 or higher](https://adoptium.net/)
* [Gradle](https://gradle.org/) (Optional as this project ships with the Gradle wrapper)

### Running the Application on Your Local Machine

* Make sure you have Docker up- and running (`docker info`) and Docker Compose installed (`docker-compose -v`)
* Start the required infrastructure with `docker-compose up`
* Run `./gradlew bootRun` to start the application
* Access http://localhost:8080 in your browser

You can now log in with the following users: `duke`, `tom`, `bjoern`, `philip`. They all have the same password `stratospheric`.

### Application Profiles

- `dev` running the application locally for development. You don't need any AWS account or running AWS services for this. All infrastructure components are started within `docker-compose.yml`.
- `aws` running the application inside AWS. This requires the whole infrastructure setup inside your AWS account.

### Running the Tests

Run `./gradlew build` from the command line.

### Deployment

You can deploy the application by using the standard Spring Boot deployment mechanism (see these three articles for more
information on Spring Boot deployment techniques and alternatives:
[Deploying Spring Boot Applications](https://spring.io/blog/2014/03/07/deploying-spring-boot-applications),
[Running your application](https://docs.spring.io/spring-boot/docs/current/reference/html/using-boot-running-your-application.html),
[Installing Spring Boot applications](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment-install.html)):

## Architecture

### Model

#### Class structure
![alt text][class-diagram]

#### Entity-relationship
![alt text][entity-relationship-diagram]

#### Database schema
![alt text][database-schema-diagram]

[class-diagram]:https://github.com/stratospheric-dev/stratospheric/raw/main/application/docs/Todo%20App%20-%20Class%20Diagram.png "class diagram"
[entity-relationship-diagram]:https://github.com/stratospheric-dev/stratospheric/raw/main/application/docs/Todo%20App%20-%20ER%20diagram.png "entity-relationship diagram"
[database-schema-diagram]:https://github.com/stratospheric-dev/stratospheric/raw/main/application/docs/Todo%20App%20-%20ER%20diagram%20from%20database%20schema.png "database schema diagram"

## Built with

* [Spring Boot](https://projects.spring.io/spring-boot/) and the following starters: Spring Web MVC, Spring Data JPA, Spring Cloud AWS, Spring WebFlux, Spring WebSocket, Thymeleaf, Spring Mail, Spring Validation, Spring Security, Actuator, OAuth2 Client
* [Gradle](https://gradle.org/)

## License

[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)

## Authors

* **[Tom Hombergs](https://reflectoring.io)**
* **[Philip Riecks](https://rieckpil.de)**
* **[Björn Wilmsmann](https://bjoernkw.com)**



## Running the app on AWS

To bootstrap our AWS environment

```bash
npm run bootstrap
```

```bash
cd ../application
gradle -x test build
aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com
docker buildx build --platform=linux/amd64 -t ${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/todo-app:DYNAMICALLY_OVERRIDDEN_BY_WORKFLOW --push .

cd ../cdk
# Shell A
npm run network:deploy; npm run database:deploy; npm run activeMq:deploy; say "complété dans A"

# In another shell
npm run repository:deploy; npm run messaging:deploy; npm run cognito:deploy; say "complété dans B"

# Shell A
npm run domain:deploy; npm run service:deploy; npm run monitoring:deploy; npm run canary:deploy; say "complété dans A"
```

## Destroying stacks from AWS
```bash
# [!] Prior to this, we might need to a) delete the Service-Parameters stack ; b) clean up a DB subNet group from the DB; and c) to delete the User Pool via the AWS console. 
npm run canary:destroy; npm run monitoring:destroy; npm run service:destroy; npm run domain:destroy; npm run cognito:destroy; npm run messaging:destroy; npm run repository:destroy; npm run activeMq:destroy; npm run database:destroy; npm run network:destroy; say "complété"
```

### To delete the CDKToolkit stack:

1. open its "Resources" tab from the CloudFormation.
2. Within the resource tab, copy the "Physical ID" of the AWS::S3::Bucket.
3. Within the S3 Management Console, select that same bucket and click the "Empty" button.
4. For that bucket click the "Delete" button.
5. Back in the CDKToolkit stack within the CloudFormation window, click the "Delete" button.
