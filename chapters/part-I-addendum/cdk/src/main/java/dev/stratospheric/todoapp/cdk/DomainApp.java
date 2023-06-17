package dev.stratospheric.todoapp.cdk;

import dev.stratospheric.cdk.ApplicationEnvironment;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;

/**
 * [N]:part-I-addendum]:a_record - Creates a DNS "A record" for associating the "app.hjoly_stratos.dev" Custom Domain for the ELB. This is for enabling users to access the to-do application via that custom domain. This CDK app works for any domain as long as it is managed by Route53.
 */
public class DomainApp {

  public static void main(final String[] args) {
    App app = new App();

    String environmentName = (String) app.getNode().tryGetContext("environmentName");
    Validations.requireNonEmpty(environmentName, "context variable 'environmentName' must not be null");

    String applicationName = (String) app.getNode().tryGetContext("applicationName");
    Validations.requireNonEmpty(applicationName, "context variable 'applicationName' must not be null");

    String accountId = (String) app.getNode().tryGetContext("accountId");
    Validations.requireNonEmpty(accountId, "context variable 'accountId' must not be null");

    String region = (String) app.getNode().tryGetContext("region");
    Validations.requireNonEmpty(region, "context variable 'region' must not be null");

    // [N] Specifies the domain name for which we want to create the SSL certificate.  This will be the domain our users will access our application by later on, for example, app.hjoly_stratos.dev
    String hostedZoneDomain = (String) app.getNode().tryGetContext("hostedZoneDomain");
    Validations.requireNonEmpty(hostedZoneDomain, "context variable 'hostedZoneDomain' must not be null");

    // [N] Specifies the domain name for which we want to create the SSL certificate.  This will be the domain our users will access our application by later on, for example, app.hjoly_stratos.dev
    String applicationDomain = (String) app.getNode().tryGetContext("applicationDomain");
    Validations.requireNonEmpty(applicationDomain, "context variable 'applicationDomain' must not be null");

    Environment awsEnvironment = makeEnv(accountId, region);

    ApplicationEnvironment applicationEnvironment = new ApplicationEnvironment(
      applicationName,
      environmentName
    );

    new DomainStack(app, "domain", awsEnvironment, applicationEnvironment, hostedZoneDomain, applicationDomain);

    app.synth();
  }

  static Environment makeEnv(String account, String region) {
    return Environment.builder()
      .account(account)
      .region(region)
      .build();
  }
}
