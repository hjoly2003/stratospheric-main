# Chapter 6

## Summary

In this chapter, we answer the following questions:
• How can we create reusable CDK constructs?
• How do we integrate such reusable constructs in our CDK apps?
• How can we design an easy-to-maintain CDK project?

![1685569539412](image/README/1685569539412.png)

The image above shows what we want to achieve. Each box is a CloudFormation resource (or a set of CloudFormation resources) that we want to deploy.

* *Docker Repository* stack - The underlying AWS service we’re using here is ECR - Elastic Container Registry.
* The *Network* stack - It deploys a VPC (Virtual Private Network) with a *public subnet* and an *isolated (private) subnet*. The public subnet now contains an Application Load Balancer (ALB) that forwards incoming traffic to an ECS (Elastic Container Service) Cluster - the runtime of our application. The isolated subnet is not accessible from the outside and is designed to secure internal resources such as our database.
* The *Service* stack - contains an *ECS service* and an *ECS task*. Remember that an ECS task is basically a Docker image with a few additional configurations, and an ECS service wraps one or several such tasks.

## Playing with the CDK Apps

```bash
npm install # to install the dependencies
cdk bootstrap aws://<ACCOUNT_ID>/<REGION> # to bootstrap our AWS environment
```

Check the parameters inside the cdk.json (most importantly, set the account ID to your AWS account ID).

Since we're building the application on an arm64 platform, we need to create a Docker image compatible to the default AWS ECS architecture (see [How to build docker image for multiple platforms with cross-compile?](https://stackoverflow.com/questions/73978929/how-to-build-docker-image-for-multiple-platforms-with-cross-compile)):

```bash
cd chapter-6/application
gradle build
# The following requires Docker to be running: it builds the image in x86_64 and push it to DockerHub
docker buildx build --platform=linux/amd64 -t hjolydocker/todo-app-v1:latest --push .
```

What’s left to do is to trigger a redeployment of our network and service apps:

```bash
cd chapter-6/cdk
npm run network:deploy 
npm run service:deploy
```

To see the todo application in your brower, you can access its public URL under the Elastic Container Service in the AWS Console
(*Networking* tab -> "Clusters > YOUR_CLUSTER > Services >
YOUR_SERVICE" -> "DNS names").

Then, have a look around in the AWS Console to see the resources those
commands created.

Don't forget to delete the stacks afterward:

```bash
npm run service:destroy
npm run network:destroy
```

To delete the CDKToolkit stack:

1. open its "Resources" tab from the CloudFormation.
2. Within the resource tab, copy the "Physical ID" of the AWS::S3::Bucket.
3. Within the S3 Management Console, select that same bucket and click the "Empty" button.
4. For that bucket click the "Delete" button.
5. Back in the CDKToolkit stack within the CloudFormation window, click the "Delete" button.
