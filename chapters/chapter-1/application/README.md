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

get to the url of the mentionned service.
When done...

```bash
cd ../cloudformation
./delete.sh
```
