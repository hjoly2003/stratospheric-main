package dev.stratospheric.todoapp.cdk;

import dev.stratospheric.cdk.Network;
import dev.stratospheric.cdk.Network.NetworkInputParameters;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

public class NetworkApp {

  /**
   * @param args Must contain the environmentName ("staging" or "prod"), the AWS accountID and region. Can contain sslCertificateArn.
   */
  public static void main(final String[] args) {
    App app = new App();

    // [N] Either "staging" or "production". The environmentName is used to be able to create multiple stacks for different environments from the same CDK app.
    String environmentName = (String) app.getNode().tryGetContext("environmentName");
    Validations.requireNonEmpty(environmentName, "context variable 'environmentName' must not be null");

    // [N] AWS account ID asscociated to the IAM user account.
    String accountId = (String) app.getNode().tryGetContext("accountId");
    Validations.requireNonEmpty(accountId, "context variable 'accountId' must not be null");

    String region = (String) app.getNode().tryGetContext("region");
    Validations.requireNonEmpty(region, "context variable 'region' must not be null");

    String sslCertificateArn = (String) app.getNode().tryGetContext("sslCertificateArn");

    Environment awsEnvironment = makeEnv(accountId, region);

    Stack networkStack = new Stack(app, "NetworkStack", StackProps.builder()
      // We use the environmentName in the stackName() method to prefix the name of the stack. This separates the stack and the other resources from those deployed in another environment.
      .stackName(environmentName + "-Network")
      .env(awsEnvironment)
      .build());

    NetworkInputParameters inputParameters = new NetworkInputParameters();

    if(sslCertificateArn != null && !sslCertificateArn.isEmpty()){
      inputParameters.withSslCertificateArn(sslCertificateArn);
    }

    // [N]:part-I-addendum]:cert - We pass the sslCertificateArn to the Network construct through the inputParameters. The later will create two ELB listeners: one https listener on port 443 using that SSL certificate and another http listener on port 80 (see https://github.com/stratospheric-dev/cdk-constructs/blob/main/src/main/java/dev/stratospheric/cdk/Network.java lines 313-324). 
    // [N]:part-I-addendum]:https-redirect - To avoid any insecure traffic, we only allow access via HTTPS. For this to work, we automatically redirect any unencrypted traffic to HTTPS (see https://github.com/stratospheric-dev/cdk-constructs/blob/main/src/main/java/dev/stratospheric/cdk/Network.java lines 326-341).
    Network network = new Network(
      networkStack,
      "Network",
      awsEnvironment,
      environmentName,
      inputParameters);

    app.synth();
  }

  static Environment makeEnv(String account, String region) {
    return Environment.builder()
      .account(account)
      .region(region)
      .build();
  }

}
