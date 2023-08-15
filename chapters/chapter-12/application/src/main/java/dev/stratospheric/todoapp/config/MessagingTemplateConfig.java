package dev.stratospheric.todoapp.config;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Configuration
public class MessagingTemplateConfig {

  /**
   * [N]:sqs - Returns an {@code SqsTemplate} bean to set our Spring Boot application up for SQS.<p/>
   * Spring Cloud AWS auto-configures the Amazon SQS Java SDK {@code AmazonSQSAsync} for us.
   */
  @Bean
  public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient) {
    return SqsTemplate.newTemplate(sqsAsyncClient);
  }
}
