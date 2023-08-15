# Stratospheric - Chapter 11: Connecting to a Database with

## Amazon RDS

]:rds - In the sample Todo application, we’ll be using a database for storing todos as well as for managing users and sharing todos, and for that we’ve opted for a PostgreSQL RDBMS managed by Amazon Relational Database Service (RDS).

]:kms - We'll use AWS Key Management Service (KMS) for encrypting sensitive data at rest.

]:local]:postgres  - When running the application locally, we’ll be using a PostgreSQL database, for emulating AWS RDS.

]:spring]:jpa - The application uses Spring Data JPA to store data in a PostgreSQL database.

]:flyway - We use Flyway for setting up and subsequently migrating the application database to the state required by the current application source code. The `resources` folder contains a sub-directory named `db/migration/postgresql` with the Flyway migration SQL scripts for setting up the application database.

## Running the app

### For Local Development

```bash
cd chapter-11/application
./gradlew build
docker-compose up
./gradlew bootRun
```

Open your browser to `http://localhost:8080/`.

### Deploying to AWS

To bootstrap our AWS environment

```bash
cdk bootstrap aws://<ACCOUNT_ID>/<REGION>
```

We first need to bootstrap the Elastic Container Registry (ECS) so that, later, it will store our application image.
```bash
cd chapter-11/cdk
npm run repository:deploy
```

Since we're building the application on an arm64 platform, we need to create a Docker image compatible to the default AWS ECS architecture

First, start docker.

```bash
cd chapter-11/application
gradle build
aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com
# The following requires Docker to be running: it builds the image in x86_64 and push it to DockerHub
docker buildx build --platform=linux/amd64 -t ${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/todo-app:1 --push .
```

#### Deploying the Generated CDK App:

```bash
cd chapter-11/cdk
npm run network:deploy
npm run database:deploy
```

Next, we deploy the todo Service, and the custom domain service:

```bash
npm run service:deploy
```

You can access the todo-app public URL under the *Elastic Container Service* in the AWS Console ("Clusters > YOUR_CLUSTER > "Services" > YOUR_SERVICE" -> "Networking" tab -> "DNS names").
To conclude, have a look around in the AWS Console to see the resources those commands created.
Don't forget to delete the stacks afterward:

```bash
npm run service:destroy # Need to destroy manually the stack holding the service parameters via the AWS console.
npm run database:destroy
...
13 h 46 min 12 s | DELETE_FAILED        | AWS::RDS::DBSubnetGroup                     | DatabaseStack/Database/dbSubnetGroup
Cannot delete the subnet group 'staging-todo-app-dbsubnetgroup' because at least one database instance: staging-todo-app-database is still
using it. (Service: Rds, Status Code: 400, Request ID: ba08a2ef-9054-41e2-9dbe-cc8d54725d0a)
# I had to manually delete the sub-network.
npm run network:destroy
# Note: we first need to manually delete the docker images contained in the ECR before deleting it.
npm run repository:destroy
```

#### To delete the CDKToolkit stack:

1. open its "Resources" tab from the CloudFormation.
2. Within the resource tab, copy the "Physical ID" of the AWS::S3::Bucket.
3. Within the S3 Management Console, select that same bucket and click the "Empty" button.
4. For that bucket click the "Delete" button.
5. Back in the CDKToolkit stack within the CloudFormation window, click the "Delete" button.
