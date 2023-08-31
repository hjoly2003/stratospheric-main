package dev.stratospheric.todoapp.tracing;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/** 
 * [N]:nsql]:web-trace - Model class used to store data from our Java application into our DynamoDB table.<p/>
 * The {@code @DynamoDbBean} annotation from the DynamoDB SDK marks a class as a representation of a DynamoDB table.
 */
@DynamoDbBean
public class Breadcrumb {

  private String id;
  private String uri;
  private String username;
  private String timestamp;

  /**
   * The {@code @DynamoDbPartitionKey} annotation denotes the id attribute to be used as the table’s primary key.
   * @return
   */
  @DynamoDbPartitionKey
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  // [N] The {@code @DynamoDbAttribute} annotation designates the attributes in the mapped Dynamo DB. 
  @DynamoDbAttribute(value = "uri")
  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  /**
   * The {@code username} partition key allows us to query for a list of all {@code Breadcrumb} items for a specific user as well as a list of events for a user within a specific time frame.
   * @return
   */
  @DynamoDbAttribute(value = "username")
  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * The {@code timestamp} is a sort key which allows to aggregate data by a partition key
   * @return
   */
  @DynamoDbAttribute(value = "timestamp")
  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }
}
