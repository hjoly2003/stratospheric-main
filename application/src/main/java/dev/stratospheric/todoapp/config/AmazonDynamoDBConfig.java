package dev.stratospheric.todoapp.config;

import io.awspring.cloud.dynamodb.DynamoDbTableNameResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Locale;

/**
 * [N]:nosql - A custom implementation of the {@link DynamoDbTableNameResolver} to deploy our application to multiple stages and access a unique table per stage.<p/>
 * This custom implementation prefixes the sanitized and lowercased classname of the DynamoDB mapping class with the application’s name and environment. This allows us to prefix our table name with the environment and application name.
 */
@Configuration
public class AmazonDynamoDBConfig {

  @Bean
  public DynamoDbTableNameResolver dynamoDbTableNameResolver(Environment environment) {
    String environmentName = environment.getProperty("custom.environment");
    String applicationName = environment.getProperty("spring.application.name");

    return new DynamoDbTableNameResolver() {
      @Override
      public <T> String resolve(Class<T> clazz) {
        String className = clazz.getSimpleName().replaceAll("(.)(\\p{Lu})", "$1_$2").toLowerCase(Locale.ROOT);
        return environmentName + "-" + applicationName + "-" + className;
      }
    };
  }
}

