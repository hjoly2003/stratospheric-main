package dev.stratospheric.todoapp.collaboration;

import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

/**
 * [N]:sqs]:share]:receiver - The actual listener of the SQS queue. 
 */
@Component
public class TodoSharingListener {

  private final MailSender mailSender;
  private final TodoCollaborationService todoCollaborationService;
  private final boolean autoConfirmCollaborations;
  private final String confirmEmailFromAddress;
  private final String externalUrl;

  private static final Logger LOG = LoggerFactory.getLogger(TodoSharingListener.class.getName());

  /**
   * 
   * @param mailSender [N]:ses - Injects the auto-configured {@code MailSender} into our {@code TodoSharingListener}.
   * @param todoCollaborationService
   * @param autoConfirmCollaborations [N]:local]:share - To auto-confirm collaborations locally. {@code custom.auto-confirm-collaborations} is defined in {@code application.yml}.
   * @param confirmEmailFromAddress [N] The sender of the confirmation email.
   * @param externalUrl [N] The URL of the endpoint that a prospect collaborator will use to confirm a collaboration request.
   */
  public TodoSharingListener(
    MailSender mailSender,
    TodoCollaborationService todoCollaborationService,
    @Value("${custom.auto-confirm-collaborations}") boolean autoConfirmCollaborations,
    @Value("${custom.confirm-email-from-address}") String confirmEmailFromAddress,
    @Value("${custom.external-url}") String externalUrl
  ) {
    this.mailSender = mailSender;
    this.todoCollaborationService = todoCollaborationService;
    this.autoConfirmCollaborations = autoConfirmCollaborations;
    this.confirmEmailFromAddress = confirmEmailFromAddress;
    this.externalUrl = externalUrl;
  }

  /**
   * [N]:sqs]:listener - Listen the SQS queue for sharing messages.<p/>
   * Since it uses the {@code @SqsListener} annotation, the method acts as an SQS handler method. This annotation expects a list of logical or physical SQS queue names (only one in our case).<p/>
   * Each message received from SQS needs to be acknowledged and deleting it does the job of notifying the queue that it has been delivered successfully. For annotation-driven SQS listeners, we can specify a deletion policy. For example:<p/> 
   * <em>@SqsListener(value = "${custom.sharing-queue}", deletionPolicy = NEVER)</em><p/>
   * In our case, we use {@code SqsMessageDeletionPolicy#NO_REDRIVE}, the default deletion policy as defined by Spring Cloud AWS.
   * @param payload The {@code TodoCollaborationNotification}.  Spring Cloud AWS, and in this particular case its Spring Messaging integration, is responsible for extracting the message payload and resolving it using a {@code PayloadMethodArgumentResolver}. 
   * @see <a href="https://docs.awspring.io/spring-cloud-aws/docs/current/apidocs/io/awspring/cloud/messaging/listener/SqsMessageDeletionPolicy.html#NO_REDRIVE">io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy#NO_REDRIVE</a>
   */
  @SqsListener(value = "${custom.sharing-queue}")
  public void listenToSharingMessages(TodoCollaborationNotification payload) throws InterruptedException {
    LOG.info("Incoming todo sharing payload: {}", payload);

    // [N]:ses - Creates the email message based on the incoming payload.
    SimpleMailMessage message = new SimpleMailMessage();
    // [N] As we’ve confirmed the domain identity for our hjolystratos.net domain, we can use confirmEmailFromAddress as the email sender.
    message.setFrom(confirmEmailFromAddress);
    message.setTo(payload.getCollaboratorEmail());
    message.setSubject("A todo was shared with you");
    message.setText(
      String.format(
        // [N] The link, included in the email, targets an endpoint of our Spring Boot backend to confirm the collaboration. This endpoint requires authentication and hence the collaborator needs to be logged in. See TodoCollaborationController#confirmCollaboration
        """
          Hi %s,\s

          someone shared a Todo from %s with you.

          Information about the shared Todo item:\s

          Title: %s\s
          Description: %s\s
          Priority: %s\s

          You can accept the collaboration by clicking this link: %s/todo/%s/collaborations/%s/confirm?token=%s\s

          Kind regards,\s
          Stratospheric""",
        payload.getCollaboratorEmail(),
        externalUrl,
        payload.getTodoTitle(),
        payload.getTodoDescription(),
        payload.getTodoPriority(),
        externalUrl,
        payload.getTodoId(),
        payload.getCollaboratorId(),
        payload.getToken()
      )
    );
    mailSender.send(message);

    LOG.info("Successfully informed collaborator about shared todo.");

    // [N] In the local configuration, we auto-confirm collaborations.
    if (autoConfirmCollaborations) {
      LOG.info("Auto-confirmed collaboration request for todo: {}", payload.getTodoId());
      Thread.sleep(2_500);
      todoCollaborationService.confirmCollaboration(payload.getCollaboratorEmail(), payload.getTodoId(), payload.getCollaboratorId(), payload.getToken());
    }
  }
}
