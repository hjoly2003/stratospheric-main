package dev.stratospheric.todoapp.cdk;

import dev.stratospheric.cdk.ApplicationEnvironment;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableEncryption;
import software.amazon.awscdk.services.dynamodb.TableProps;
import software.constructs.Construct;

/**
 * [N]:nosql]:web-trace - The breadcrumb table for tracing the user’s journey through our application.<p/>
 * The Table class is an AWS CDK level 2 construct that abstracts the underlying CloudFormation properties.
 */
public class BreadcrumbsDynamoDbTable extends Construct {

  public BreadcrumbsDynamoDbTable(
    final Construct scope,
    final String id,
    final ApplicationEnvironment applicationEnvironment,
    final InputParameter inputParameters
  ) {

    super(scope, id);

    new Table(
      this,
      "BreadcrumbsDynamoDbTable",
      TableProps.builder()
        // [N]:nosql - Defines the partition key which acts as the primary key of our table and translates to the id attribute of our entity (aka “item”).
        .partitionKey(
          Attribute.builder().type(AttributeType.STRING).name("id").build())
        // [N] Usage of record, a jdk14 feature
        .tableName(applicationEnvironment.prefix(inputParameters.tableName))
        .encryption(TableEncryption.AWS_MANAGED)
        .billingMode(BillingMode.PROVISIONED)
        .readCapacity(10)
        .writeCapacity(10)
        .removalPolicy(RemovalPolicy.DESTROY)
        .build());
  }

  // [N] record is a jdk14 feature for encupsulating final data (see https://www.baeldung.com/java-record-keyword)
  record InputParameter(String tableName) {
  }
}
