package dev.stratospheric.todoapp.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * [N]:log]:custom-fields - Configure Spring to use our {@code LoggingContextInterceptor}.<p/>
 * Implementing a {@link WebMvcConfigurer} lets us add our {@code LoggingContextInterceptor} to Spring’s {@link InterceptorRegistry}, thereby allowing it to intercept all incoming web requests.
 */
@Component
class LoggingContextConfiguration implements WebMvcConfigurer {

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new LoggingContextInterceptor());
  }
}
