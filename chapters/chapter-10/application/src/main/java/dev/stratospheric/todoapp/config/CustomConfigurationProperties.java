package dev.stratospheric.todoapp.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

@ConfigurationProperties(prefix = "custom")
@Validated
class CustomConfigurationProperties {

  @NotEmpty
  private Set<String> invitationCodes;

  private boolean useCognitoAsIdentityProvider;

  public Set<String> getInvitationCodes() {
    return invitationCodes;
  }

  public void setInvitationCodes(Set<String> invitationCodes) {
    this.invitationCodes = invitationCodes;
  }

  public Boolean getUseCognitoAsIdentityProvider() {
    return useCognitoAsIdentityProvider;
  }

  public void setUseCognitoAsIdentityProvider(Boolean useCognitoAsIdentityProvider) {
    this.useCognitoAsIdentityProvider = useCognitoAsIdentityProvider;
  }

}
