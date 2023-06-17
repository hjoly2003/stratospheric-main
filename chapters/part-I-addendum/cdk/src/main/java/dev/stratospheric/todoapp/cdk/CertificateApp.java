package dev.stratospheric.todoapp.cdk;

import dev.stratospheric.cdk.ApplicationEnvironment;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;

/**
 * [N]:part-I-addendum]:cert
 */
public class CertificateApp {

  public static void main(final String[] args) {
    App app = new App();

    // [N] The following context variables are defined in $ROOT/cdk/cdk.json

    String environmentName = (String) app.getNode().tryGetContext("environmentName");
    Validations.requireNonEmpty(environmentName, "context variable 'environmentName' must not be null");

    String applicationName = (String) app.getNode().tryGetContext("applicationName");
    Validations.requireNonEmpty(applicationName, "context variable 'applicationName' must not be null");

    String accountId = (String) app.getNode().tryGetContext("accountId");
    Validations.requireNonEmpty(accountId, "context variable 'accountId' must not be null");

    String region = (String) app.getNode().tryGetContext("region");
    Validations.requireNonEmpty(region, "context variable 'region' must not be null");

    // [N] Specifies the domain name for which we want to create the SSL certificate.  This will be the domain our users will access our application by later on, for example, app.hjoly_stratos.dev
    String applicationDomain = (String) app.getNode().tryGetContext("applicationDomain");
    Validations.requireNonEmpty(applicationDomain, "context variable 'applicationDomain' must not be null");

    // [N] Refers to the name of the hosted zone within Route53. For our example, that’s stratospheric.dev.
    String hostedZoneDomain = (String) app.getNode().tryGetContext("hostedZoneDomain");
    Validations.requireNonEmpty(hostedZoneDomain, "context variable 'hostedZoneDomain' must not be null");

    Environment awsEnvironment = makeEnv(accountId, region);

    ApplicationEnvironment applicationEnvironment = new ApplicationEnvironment(
      applicationName,
      environmentName
    );

    new CertificateStack(app, "certificate", awsEnvironment, applicationEnvironment, applicationDomain, hostedZoneDomain);

    app.synth();
  }

  static Environment makeEnv(String account, String region) {
    return Environment.builder()
      .account(account)
      .region(region)
      .build();
  }
}
