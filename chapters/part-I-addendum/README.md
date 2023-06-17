    

# Part I Addendum: Configuring HTTPS and a Custom Domain with Route 53 and ELB

## Summary

In this chapter, we’ll add a custom domain to our application and create and
install an SSL certificate for that domain.

## Playing with the CDK Apps

```bash
npm install # to install the dependencies
cdk bootstrap aws://<ACCOUNT_ID>/<REGION> # to bootstrap our AWS environment
```

Check the parameters inside the cdk.json (most importantly, set the account ID to your AWS account ID).

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
npm run repository:deploy 
npm run network:deploy 
npm run service:deploy
npm run domain:deploy # creates an A-record for our subdomain via Route53 (takes a while)
```

From now on, the sample Todo application is accessible from our custom domain using HTTPS: `https://app.hjoly_stratos.dev`. Have a look around in the AWS Console to see the resources those
commands created.

Don't forget to delete the stacks afterward:

```bash
npm run domain:destroy
npm run service:destroy
npm run network:destroy
npm run repository:destroy
npm run certificate:destroy
```

1. To delete the CDKToolkit stack, open its "Resources" tab from the CloudFormation.
2. Within the resource tab, copy the "Physical ID" of the AWS::S3::Bucket.
3. Within the S3 Management Console, select that same bucket and click the "Empty" button.
4. For that bucket click the "Delete" button.
5. Back in the CDKToolkit stack within the CloudFormation window, click the "Delete" button.
