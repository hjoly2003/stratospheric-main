# Stratospheric - Chapter 1: Getting Warm with AWS

The first version of the Todo app contains only a static page. We're using this app to demonstrate how to deploy a Spring Boot application to AWS.

## Running the app locally

Run ``gradle bootRun`` from the command line.

Open your browser to `http://localhost:8080/`.

## Installing the AWS CLI

Type the following:

```
aws configure
```

You will be asked to provide 4 parameters:

* AWS Access Key ID [****************Kweu]:
* AWS Secret Access Key [****************CmqH]:
* Default region name [ap-southeast-2]:
* Default output format (yaml or json)

You can get the “AWS Access Key ID” and “AWS Secret Access Key” after you have logged into your AWS account when you click on your account name and then “My Security Credentials”.

## Docker

This app is published as a Docker image at https://hub.docker.com/repository/docker/stratospheric/todo-app-v1.

## Publishing the app to Docker Hub

Since we're building the application on an arm64 platform, we need to create a Docker image compatible to the default AWS ECS architecture (see [How to build docker image for multiple platforms with cross-compile?](https://stackoverflow.com/questions/73978929/how-to-build-docker-image-for-multiple-platforms-with-cross-compile)):

```bash
cd chapter-1/application
gradle build
# The following requires Docker to be running: it builds the image in x86_64 and push it to DockerHub
docker buildx build --platform=linux/amd64 -t hjolydocker/todo-app-v1:latest --push .
```

## Deploying the app to AWS

```bash
cd ../cloudformation
./create.sh
```

The output of the script should look something like that:

```bash
StackId: arn:aws:cloudformation:.../stratospheric-ecs-basic-network/...
StackId: arn:aws:cloudformation:.../stratospheric-ecs-basic-service/...
ECS Cluster: stratospheric-ecs-basic-network-ECSCluster-qqX6Swdw54PP
ECS Task: arn:aws:ecs:.../stratospheric-ecs-basic-network-...
Network Interface: eni-02c096ce1faa5ecb9
Public IP: 13.55.30.162
You can access your service at http://13.55.30.162:8080
```

get to the url of the mentionned service. Note that the public IP can be found from the AWS Console:

1. Go to the *CloudFormation Stacks* and select the `stratospheric-basic-network` service.
2. In the `stratospheric-basic-network` page, get to the *Resources* tab.
3. In that page, click the *Physical ID* of the *ECSCluster* named something like `stratospheric-basic-network-ECSCluster-bDAi5JNxnhwK`.
4. Within the *Amazon Elastic Container Service Cluster* page of `stratospheric-basic-network-ECSCluster-bDAi5JNxnhwK`, click the *Tasks* tab.
5. Within that tab, click the link of the only task which is running. The link is named something like `4b506f05ab6645cf9e607b2a9683fedc`.
6. Within the `4b506f05ab6645cf9e607b2a9683fedc` task *Configuration* page, in the *Configuration* section, you should see the *Public IP* that we're looking for.

Another way (verified) to get the public URL: under the Elastic Container Service in the AWS Console
(*Networking* tab -> "Clusters > YOUR_CLUSTER > Services >
YOUR_SERVICE" -> "DNS names")

When done...

```bash
cd ../cloudformation
./delete.sh
```
