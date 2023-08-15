package dev.stratospheric.todoapp.registration;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;

/**
 * [N]:security]:usr-signup - A basic Spring MVC controller for exposing the user registration view.
 */
@Controller
@RequestMapping("/register")
public class RegistrationController {

  private final RegistrationService registrationService;

  public RegistrationController(RegistrationService registrationService) {
    this.registrationService = registrationService;
  }

  /**
   * [N] Instantiates an empty {@link Registration} object to be filled by the user.
   */
  @GetMapping
  public String getRegisterView(Model model) {
    model.addAttribute("registration", new Registration());
    return "register";
  }

  /**
   * [N] End point for submitting a user registration
   * @param registration
   * @param bindingResult [?] Used to ensure that our model matches our validation rules: no empty email or username (as definded through the {@code @NotEmpty} annotation in the {@link Person} model).
   * @param model
   * @param redirectAttributes
   * @return
   */
  @PostMapping
  public String registerUser(@Valid Registration registration,
                             BindingResult bindingResult,
                             Model model, RedirectAttributes redirectAttributes) {
    
    // [?] Ensures that our model matches our validation rules: no empty email or username (as definded through the @NotEmpty annotation in the Person model).
    if (bindingResult.hasErrors()) {
      model.addAttribute("registration", registration);
      return "register";
    }

    try {
      registrationService.registerUser(registration);

      redirectAttributes.addFlashAttribute("message",
        "You successfully registered for the Todo App. " +
          "Please check your email inbox for further instructions."
      );
      redirectAttributes.addFlashAttribute("messageType", "success");

      return "redirect:/";
    } catch (CognitoIdentityProviderException exception) {

      model.addAttribute("registration", registration);
      model.addAttribute("message", exception.getMessage());
      model.addAttribute("messageType", "danger");

      return "register";
    }
  }
}
