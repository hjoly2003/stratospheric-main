package dev.stratospheric.todoapp.tracing;

import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * [N]:nosql]:spring-evnt - A DAO which depends on the {@link DynamoDbTemplate} bean to create a new {@code Breadcrumb} item whenever a {@code TracingEvent} is emitted. 
 */
@Component
public class TraceDao {

  private static final Logger LOG = LoggerFactory.getLogger(TraceDao.class);

  private final DynamoDbTemplate dynamoDbTemplate;

  public TraceDao(DynamoDbTemplate dynamoDbTemplate) {
    this.dynamoDbTemplate = dynamoDbTemplate;
  }

  /**
   * [N]:spring-evnt - Stores a {@code TracingEvent}.<p/>
   * It uses the {@code @EventListener} Spring annotation to listen to a {@code TracingEvent}, which is a specific type of {@link ApplicationEvent}.<p/>
   * We use the {@code @Async} annotation to consume the {@code TracingEvent} asynchronously without blocking the thread and potentially delaying the incoming request.
   * @param tracingEvent
   */
  @Async
  @EventListener(TracingEvent.class)
  public void storeTracingEvent(TracingEvent tracingEvent) {
    Breadcrumb breadcrumb = new Breadcrumb();
    breadcrumb.setId(UUID.randomUUID().toString());
    breadcrumb.setUri(tracingEvent.getUri());
    breadcrumb.setUsername(tracingEvent.getUsername());
    breadcrumb.setTimestamp(ZonedDateTime.now().toString());

    dynamoDbTemplate.save(breadcrumb);

    LOG.info("Successfully stored breadcrumb trace");
  }

  /**
   * [N]:spring-evnt - Find all events for a given {@code username}.<p/>
   * @param username
   * @return
   */
  public List<Breadcrumb> findAllEventsForUser(String username) {
    Breadcrumb breadcrumb = new Breadcrumb();
    breadcrumb.setUsername(username);

    return dynamoDbTemplate.query(
        QueryEnhancedRequest
          .builder()
          .queryConditional(
            QueryConditional.keyEqualTo(
              Key
                .builder()
                .partitionValue(breadcrumb.getId())
                .build()
            )
          )
          .build(),
        Breadcrumb.class
      ).items()
      .stream()
      .toList();
  }

  /**
   * [N]:spring-evnt - Find all events for a given {@code username} for the last two weeks.<p/>
   * @param username
   * @return
   */
  public List<Breadcrumb> findUserTraceForLastTwoWeeks(String username) {
    ZonedDateTime twoWeeksAgo = ZonedDateTime.now().minusWeeks(2);

    Breadcrumb breadcrumb = new Breadcrumb();
    breadcrumb.setUsername(username);

    return dynamoDbTemplate.query(
        QueryEnhancedRequest
          .builder()
          .queryConditional(
            QueryConditional.keyEqualTo(
              Key
                .builder()
                .partitionValue(breadcrumb.getId())
                .build()
            )
          )
          .filterExpression(
            Expression
              .builder()
              .expression("timestamp > :twoWeeksAgo")
              .putExpressionValue(":twoWeeksAgo",
                AttributeValue
                  .builder()
                  .s(twoWeeksAgo.toString())
                  .build()
              )
              .build()
          )
          .build(),
        Breadcrumb.class
      )
      .items()
      .stream()
      .toList();
  }
}
