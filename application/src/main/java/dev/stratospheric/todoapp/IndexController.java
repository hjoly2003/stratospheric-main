package dev.stratospheric.todoapp;

import dev.stratospheric.todoapp.tracing.TracingEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

/**
 * [N]:thymeleaf - A public endpoint that exposes a Thymeleaf view.<p/>
 * This Spring MVC {@code @Controller } resolves the index view located inside {@code src/main/resources/templates}.<p/>
 */
@Controller
public class IndexController {

  private final ApplicationEventPublisher eventPublisher;

  public IndexController(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  /**
   * After a successful login, the user is redirected to this endpoint and Spring Security creates a Principal in the form of an OidcUser that we inject to the call. 
  * [N]:spring-evnt - Emits, via the Spring's {@code ApplicationEventPublisher}, a new {@code TracingEvent} each time a user visits the index page.
   * @param principal
   * @return
   */
  @GetMapping
  @RequestMapping("/")
  public String getIndex(Principal principal) {
    this.eventPublisher.publishEvent(
      new TracingEvent(
        this,
        "index",
        principal != null ? principal.getName() : "anonymous"
      )
    );

    return "index";
  }
}
