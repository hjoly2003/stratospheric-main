package dev.stratospheric.todoapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import dev.stratospheric.todoapp.tracing.TracingEvent;

/**
 * [N]:spring-evnt - Enables asynchronous consumption of our {@code TracingEvent}s.<p/>
 * The {@code @EnabledAsync} annotation activate asynchronous behavior in our Spring Boot application.
 */
@EnableAsync
@Configuration
public class AsyncConfig {

  /**
   * Defines a primary {@code TaskExecutor} for processing async operations on a dedicated thread pool.<p/>
   * The reason for the {@code @Primary} annotation is that we already have beans of type {@code TaskExecutor} in our {@code ApplicationContext} from Spring Cloud AWS messaging. Without marking one executor as {@code @Primary}, we wouldn’t be able to use the same executor deterministically
   * @return
   */
  @Bean
  @Primary
  public TaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setMaxPoolSize(10);
    executor.initialize();

    return executor;
  }
}
