# Welcome to your CDK Java project!

This is a blank project for Java development with CDK.

The `cdk.json` file tells the CDK Toolkit how to execute your app.

 [N] We deploy the docker repository with the following:
 ```bash
    cdk deploy \
      --app "./mvnw -e -q compile exec:java \
        -Dexec.mainClass=dev.stratospheric.todoapp.cdk.DockerRepositoryApp" \
      -c accountId=... \
      -c region=... \
      -c applicationName=...
```
With the `--app` parameter, we can define the executable that CDK should call to execute the CDK app. By default, CDK calls `mvn -e -q compile exec:java` to run an app. Having more than one CDK app in the classpath, we need to tell Maven which app to execute, so we add the `exec.mainclass` system property and point it to our `DockerRepositoryApp`. To make it a bit more convenient to execute a command with many arguments, we outsource non-sensitive configuration parameters into the `cdk.json` file.

Furthermore, we will wrap the CDK call into an NPM package. For this, we create a `package.json` file that contains a script for each command we want to run. It is a central location where we can look up the commands we have at our disposal for deploying or destroying CloudFormation stacks. 

Should the need arise, we can override a parameter in the command line with:
```bash
npm run repository:deploy -- -c applicationName=...
```
Arguments after the -- will override any arguments defined in the `package.json` script or the `cdk.json` file. We can use this mechanism to pass secrets and passwords to our apps and avoid committing any sensitive information to our GitHub repository.

It is a [Maven](https://maven.apache.org/) based project, so you can open this project with any Maven compatible Java IDE to build and run tests.

## Useful commands

 * `./mvnw package`     compile and run tests
 * `cdk ls`          list all stacks in the app
 * `cdk synth`       emits the synthesized CloudFormation template
 * `cdk deploy`      deploy this stack to your default AWS account/region
 * `cdk diff`        compare deployed stack with current state
 * `cdk docs`        open CDK documentation

Enjoy!
