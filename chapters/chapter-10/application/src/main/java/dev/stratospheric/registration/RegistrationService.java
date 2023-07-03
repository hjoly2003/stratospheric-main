package dev.stratospheric.registration;

/**
 * [N]:usr-signup - Performs the actual user registration to the RegistrationService.<p/>
 * The service implementation is in two flavors depending on which environment our application runs in (dev or aws).
 */
public interface RegistrationService {
  void registerUser(Registration registration);
}
