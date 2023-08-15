package dev.stratospheric.todoapp.registration;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * [N]:usr_signup - Annotation attached to {@link Registration#invitationCode}.<p/>
 * Plugs together the {@code @ValidInvitationCode} annotation with the {@code InvitationCodeValidator}.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = InvitationCodeValidator.class)
public @interface ValidInvitationCode {
  String message() default "Invalid invitation code. Please check it again or contact the person who invited you.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
