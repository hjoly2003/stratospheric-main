package dev.stratospheric;

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
@RequestMapping("/")
public class IndexController {

  @GetMapping
  public String getIndex(Model model, @AuthenticationPrincipal OidcUser user) {
    if (user != null) {
      model.addAttribute("claims", user.getIdToken().getClaims());
      model.addAttribute("email", user.getEmail());
    }

    return "index";
  }
}

