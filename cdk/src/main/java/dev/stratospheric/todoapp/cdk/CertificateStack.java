package dev.stratospheric.todoapp.cdk;

import dev.stratospheric.cdk.ApplicationEnvironment;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.certificatemanager.DnsValidatedCertificate;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.HostedZoneProviderProps;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.constructs.Construct;

/**
 * [N]:part-I-addendum]:cert
 */
public class CertificateStack extends Stack {

  /**
   * 
   * @param scope
   * @param id
   * @param awsEnvironment
   * @param applicationEnvironment
   * @param applicationDomain Specifies the domain name for which we want to create the SSL certificate.  This will be the domain our users will access our application by later on, for example, app.stratospheric.dev
   * @param hostedZoneDomain Refers to the name of the hosted zone within Route53. For our example, that’s stratospheric.dev.
   */
  public CertificateStack(
    final Construct scope,
    final String id,
    final Environment awsEnvironment,
    final ApplicationEnvironment applicationEnvironment,
    final String applicationDomain,
    final String hostedZoneDomain) {
    super(scope, id, StackProps.builder()
      .stackName(applicationEnvironment.prefix("Certificate"))
      .env(awsEnvironment).build());

    // [N] Retrieves the hosted zone we created while registering or transferring our domain. A  hosted zone is a container for all the DNS records belonging to a domain.
    IHostedZone hostedZone = HostedZone.fromLookup(this, "HostedZone", HostedZoneProviderProps.builder()
      .domainName(hostedZoneDomain)
      .build());

    // [N] Creates a new SSL certificate validated via DNS (a DNS-validated certificate).
    DnsValidatedCertificate websiteCertificate = DnsValidatedCertificate.Builder.create(this, "WebsiteCertificate")
      .hostedZone(hostedZone)
      .region(awsEnvironment.getRegion())
      .domainName(applicationDomain)
      .build();

    CfnOutput sslCertificateArn = new CfnOutput(this, "sslCertificateArn", CfnOutputProps.builder()
      .exportName("sslCertificateArn")
      .value(websiteCertificate.getCertificateArn())
      .build());
  }
}
