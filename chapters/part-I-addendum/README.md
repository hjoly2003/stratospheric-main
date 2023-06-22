# Part I Addendum: Configuring HTTPS and a Custom Domain with Route 53 and ELB

## Summary

In this chapter, we’ll do the following:

* ]:domain - add a custom domain to our application
* ]:cert	 - create and install an SSL certificate for that domain
* ]:https-redirect - redirect any unencrypted http calls to https
* ]:a_record - create a DNS A record to route calls from that domain to our application.

## Playing with the CDK Apps

```bash
npm install # to install the dependencies
cdk bootstrap aws://<ACCOUNT_ID>/<REGION> # to bootstrap our AWS environment
```

Check the parameters inside the cdk.json (most importantly, set the account ID to your AWS account ID).

]:domain - First, in the AWS console, we need to register a new domain with Route53. Once registered, go to the "Hosted zones" of Route53 and you should see hosted zone named according to your domain.

Since we're building the application on an arm64 platform, we need to create a Docker image compatible to the default AWS ECS architecture (see [How to build docker image for multiple platforms with cross-compile?](https://stackoverflow.com/questions/73978929/how-to-build-docker-image-for-multiple-platforms-with-cross-compile)):

```bash
cd part-I-addendum/application
gradle build
# The following requires Docker to be running: it builds the image in x86_64 and push it to DockerHub
docker buildx build --platform=linux/amd64 -t hjolydocker/todo-app-v1:latest --push .
```

To get the ARN of the SSL certificate:

```bash
cd part-I-addendum/cdk
npm run certificate:deploy
```

We should get something like

```bash
certificate (staging-todo-app-Certificate)

Outputs:
certificate.sslCertificateArn = arn:aws:acm:us-east-1:...:certificate/...
```

Lets copy that ARN into the `./cdk/cdk.json`'s `sslCertificateArn` property.

What’s left to do is to trigger a redeployment of our network and service apps:

```bash
npm run network:deploy 
npm run service:deploy # takes a while
npm run domain:deploy # creates an A-record for our subdomain via Route53 (takes a while)
```

From now on, the sample Todo application is accessible from our custom domain using HTTPS: `https://app.hjolystratos.net`. Have a look around in the AWS Console to see the resources those
commands created.

Don't forget to delete the stacks afterward:

```bash
npm run domain:destroy
npm run service:destroy
npm run network:destroy
npm run certificate:destroy
```

To delete the CDKToolkit stack:

1. open its "Resources" tab from the CloudFormation.
2. Within the resource tab, copy the "Physical ID" of the AWS::S3::Bucket.
3. Within the S3 Management Console, select that same bucket and click the "Empty" button.
4. For that bucket click the "Delete" button.
5. Back in the CDKToolkit stack within the CloudFormation window, click the "Delete" button.
