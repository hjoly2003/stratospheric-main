package dev.stratospheric.registration;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * [N]:security]:usr-signup - Models the registration of a new user. To avoid bots from auto-generating accounts, this registration process is an extra-layer of protection that requires an invitation code on each signup.
 */
public class Registration {

  @NotBlank
  private String username;

  @Email
  private String email;

  @ValidInvitationCode
  private String invitationCode;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getInvitationCode() {
    return invitationCode;
  }

  public void setInvitationCode(String invitationCode) {
    this.invitationCode = invitationCode;
  }

  @Override
  public String toString() {
    return "Registration{" +
      "username='" + username + '\'' +
      ", email='" + email + '\'' +
      ", invitationCode='" + invitationCode + '\'' +
      '}';
  }
}
