package dev.stratospheric.todoapp;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * [N]:thymeleaf - A public endpoint that exposes a Thymeleaf view.<p>
 * This Spring MVC {@code @Controller } resolves the index view located inside {@code src/main/resources/templates}.
 */
@Controller
public class IndexController {

  /**
   * After a successful login, the user is redirected to this endpoint and Spring Security creates a Principal in the form of an OidcUser that we inject to the call. 
   * @param model
   * @param user
   * @return
   */
  @GetMapping
  @RequestMapping("/")
  public String getIndex(Model model, @AuthenticationPrincipal OidcUser user) {
    if (user != null) {
      // [N] Adds Oidc attributes to the Model of the index page. For educational purpose, we show these attributes in the index page just to show what's happening.
      model.addAttribute("claims", user.getIdToken().getClaims());
      model.addAttribute("email", user.getEmail());
    }

    return "index";
  }
}
