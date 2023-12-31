package dev.stratospheric.todoapp.registration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * [N]:usr-signup]:local - Performs no user registration as it's not working locally.<p/>
 */
@Service
@ConditionalOnProperty(prefix = "custom", name = "use-cognito-as-identity-provider", havingValue = "false")
public class LocalRegistrationService implements RegistrationService {

  @Override
  public void registerUser(Registration registration) {
    // no registration as we use a local Keycloak instance with a pre-defined set of users
  }
}
