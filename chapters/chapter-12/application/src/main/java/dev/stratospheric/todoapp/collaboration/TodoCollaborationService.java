package dev.stratospheric.todoapp.collaboration;

import dev.stratospheric.todoapp.person.Person;
import dev.stratospheric.todoapp.person.PersonRepository;
import dev.stratospheric.todoapp.todo.Todo;
import dev.stratospheric.todoapp.todo.TodoRepository;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * [N]:share]:sqs - Creates a collaboration request, writes it in the database and notifies the target collaborator by the SQS queue.<p/>
 * Users will be able to share their todo with any user of our application except themselves and only if they are the todo owner. 
 */
@Service
@Transactional
public class TodoCollaborationService {

  private final TodoRepository todoRepository;
  private final PersonRepository personRepository;
  private final TodoCollaborationRequestRepository todoCollaborationRequestRepository;

  private final SqsTemplate sqsTemplate;
  private final String todoSharingQueueName;

  private static final Logger LOG = LoggerFactory.getLogger(TodoCollaborationService.class.getName());

  private static final String INVALID_TODO_ID = "Invalid todo ID: ";
  private static final String INVALID_PERSON_ID = "Invalid person ID: ";
  private static final String INVALID_PERSON_EMAIL = "Invalid person Email: ";

  /**
   * @param todoSharingQueueName [N] {@code ${custom.sharing-queue}} is defined in {@code application.yml}
   * @param todoRepository
   * @param personRepository
   * @param todoCollaborationRequestRepository
   * @param sqsTemplate
   */
  public TodoCollaborationService(
    @Value("${custom.sharing-queue}") String todoSharingQueueName,
    TodoRepository todoRepository,
    PersonRepository personRepository,
    TodoCollaborationRequestRepository todoCollaborationRequestRepository,
    SqsTemplate sqsTemplate) {
    this.todoRepository = todoRepository;
    this.personRepository = personRepository;
    this.todoCollaborationRequestRepository = todoCollaborationRequestRepository;
    this.sqsTemplate = sqsTemplate;
    this.todoSharingQueueName = todoSharingQueueName;
  }

  /**
   * [N]:share - Shares a todo iff both the todo and the collaborator exist in the database AND if the {@code todoOwnerEmail} is the owner AND not such collaboration already exists in the database. 
   * @param todoOwnerEmail The email of the todo's owner.
   * @param todoId The todo to be shared.
   * @param collaboratorId The collaborator with who we want to share.
   * @return The name of the collaborator.
   */
  public String shareWithCollaborator(String todoOwnerEmail, Long todoId, Long collaboratorId) {

    Todo todo = todoRepository
      .findByIdAndOwnerEmail(todoId, todoOwnerEmail)
      .orElseThrow(() -> new IllegalArgumentException(INVALID_TODO_ID + todoId));

    Person collaborator = personRepository
      .findById(collaboratorId)
      .orElseThrow(() -> new IllegalArgumentException(INVALID_PERSON_ID + collaboratorId));

    if (todoCollaborationRequestRepository.findByTodoAndCollaborator(todo, collaborator) != null) {
      LOG.info("Collaboration request for todo {} with collaborator {} already exists", todoId, collaboratorId);
      return collaborator.getName();
    }

    LOG.info("About to share todo with id {} with collaborator {}", todoId, collaboratorId);

    TodoCollaborationRequest collaboration = new TodoCollaborationRequest();
    // [N]:share - We create a random token for each collaboration request that is required for accepting the invite.
    String token = UUID.randomUUID().toString();
    collaboration.setToken(token);
    collaboration.setCollaborator(collaborator);
    collaboration.setTodo(todo);
    todo.getCollaborationRequests().add(collaboration);

    todoCollaborationRequestRepository.save(collaboration);

    // [N]:share]:sqs - We use the SqsTemplate to send a message to a specified SQS queue. The send() method will serialize the Java object to a JSON string before sending it to SQS. New messages will now queue up inside our SQS queue for 14 days.
    sqsTemplate.send(todoSharingQueueName, new TodoCollaborationNotification(collaboration));

    return collaborator.getName();
  }

  /**
   * Registers an invited user as a collaborator for the given todo.<p/>
   * Rejects the confirmation for one of the following reasons:<ul>
   *  <li>the logged-in user tries to confirm a collaboration request for another user,</li>
   *  <li>no collaboration request for this todo and/or user exists, or</li>
   *  <li>the confirmation token is invalid.</li>
   * </ul>
   * @param authenticatedUserEmail The email of a logged-in user
   * @param todoId
   * @param collaboratorId The targetted collaborator
   * @param token The random token required for accepting the invite.
   * @return
   */
  public boolean confirmCollaboration(String authenticatedUserEmail, Long todoId, Long collaboratorId, String token) {

    Person loggedInUser = personRepository
      .findByEmail(authenticatedUserEmail)
      .orElseThrow(() -> new IllegalArgumentException(INVALID_PERSON_EMAIL + authenticatedUserEmail));

    if (!loggedInUser.getId().equals(collaboratorId)) {
      return false;
    }

    TodoCollaborationRequest collaborationRequest = todoCollaborationRequestRepository
      .findByTodoIdAndCollaboratorId(todoId, collaboratorId);

    if (collaborationRequest == null || !collaborationRequest.getToken().equals(token)) {
      return false;
    }

    Todo todo = todoRepository
      .findById(todoId)
      .orElseThrow(() -> new IllegalArgumentException(INVALID_TODO_ID + todoId));

    todo.addCollaborator(loggedInUser);

    todoCollaborationRequestRepository.delete(collaborationRequest);

    return true;
  }
}
