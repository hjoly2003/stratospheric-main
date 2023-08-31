package dev.stratospheric.todoapp.collaboration;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * [N]:share - Takes incoming collaboration requests and passes them to the {@link TodoCollaborationService} to start the sharing process.
 */
@Controller
@RequestMapping("/todo")
public class TodoCollaborationController {

  private final TodoCollaborationService todoCollaborationService;

  public TodoCollaborationController(TodoCollaborationService todoCollaborationService) {
    this.todoCollaborationService = todoCollaborationService;
  }

  /**
   * [N]:share - An enpoint to create a collaboration.
   * @param todoId The todo to be shared.
   * @param collaboratorId The collaborator with who we want to share
   * @param user The owner of the todo
   * @param redirectAttributes Provides a way to add flash attributes for posting web notifications.
   * @return
   */
  @PostMapping("/{todoId}/collaborations/{collaboratorId}")
  public String shareTodoWithCollaborator(
    @PathVariable("todoId") Long todoId,
    @PathVariable("collaboratorId") Long collaboratorId,
    @AuthenticationPrincipal OidcUser user,
    RedirectAttributes redirectAttributes
  ) {
    String collaboratorName = todoCollaborationService.shareWithCollaborator(user.getEmail(), todoId, collaboratorId);

    // [N]:share - Emits a notification via the message.html fragment
    redirectAttributes.addFlashAttribute("message",
      String.format("You successfully shared your todo with the user %s. " +
        "Once the user accepts the invite, you'll see them as a collaborator on your todo.", collaboratorName));
    redirectAttributes.addFlashAttribute("messageType", "success");

    return "redirect:/dashboard";
  }

  /**
   * [N]:share - An endpoint of our Spring Boot backend enabling a prospect collaborator to confirm the collaboration. This endpoint requires authentication and hence the collaborator needs to be logged in.
   * @param todoId The todo to be shared.
   * @param collaboratorId The collaborator with who we want to share
   * @param token The random token required for accepting the invite.
   * @param user The owner of the todo
   * @param redirectAttributes Provides a way to add flash attributes for posting web notifications.
   * @return
   */
  @GetMapping("/{todoId}/collaborations/{collaboratorId}/confirm")
  public String confirmCollaboration(
    @PathVariable("todoId") Long todoId,
    @PathVariable("collaboratorId") Long collaboratorId,
    @RequestParam("token") String token,
    @AuthenticationPrincipal OidcUser user,
    RedirectAttributes redirectAttributes
  ) {

    if (todoCollaborationService.confirmCollaboration(user.getEmail(), todoId, collaboratorId, token)) {
      redirectAttributes.addFlashAttribute("message", "You've confirmed that you'd like to collaborate on this todo.");
      redirectAttributes.addFlashAttribute("messageType", "success");
    } else {
      redirectAttributes.addFlashAttribute("message", "Invalid collaboration request.");
      redirectAttributes.addFlashAttribute("messageType", "danger");
    }

    return "redirect:/dashboard";
  }
}
