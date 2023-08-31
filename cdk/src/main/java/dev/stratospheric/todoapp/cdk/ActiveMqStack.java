package dev.stratospheric.todoapp.cdk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.stratospheric.cdk.ApplicationEnvironment;
import dev.stratospheric.cdk.Network;
import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Fn;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.amazonmq.CfnBroker;
import software.amazon.awscdk.services.ec2.CfnSecurityGroup;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

/**
 * [N]:mq
 */
public class ActiveMqStack extends Stack {

  /** [N] To connect to the broker from the Spring Boot application backend. */
  private static final String PARAMETER_USERNAME = "activeMqUsername";
  private static final String PARAMETER_PASSWORD = "activeMqPassword";

  private static final String PARAMETER_AMQP_ENDPOINT = "amqpEndpoint";
  
  /** [N] The STOMP endpoint URL is used or connecting from our Thymeleaf frontend via JavaScript and WebSocket. */
  private static final String PARAMETER_STOMP_ENDPOINT = "stompEndpoint";

  /** [N] The security group ID is used to refer to our broker’s security group when allowing ingress access from our application. */
  private static final String PARAMETER_SECURITY_GROUP_ID = "activeMqSecurityGroupId";

  private final ApplicationEnvironment applicationEnvironment;
  private final CfnBroker broker;
  private final String username;
  private final String password;

  /**
   * [N] A specific security group for our ActiveMQ stack. It's created using CDK’s {@code CfnSecurityGroup.Builder}.
   */
  private final String securityGroupId;

  /**
   * 
   * @param scope
   * @param id
   * @param awsEnvironment
   * @param applicationEnvironment
   * @param username [N]:mq - The user that should gain access to our ActiveMQ instance. Note: the password is generated internally via a {@code PasswordGenerator}.
   */
  public ActiveMqStack(
    final Construct scope,
    final String id,
    final Environment awsEnvironment,
    final ApplicationEnvironment applicationEnvironment,
    final String username
  ) {
    super(
      scope,
      id,
      StackProps.builder()
        .stackName(applicationEnvironment.prefix("ActiveMq"))
        .env(awsEnvironment)
        .build()
    );

    this.applicationEnvironment = applicationEnvironment;
    this.username = username;
    this.password = generatePassword();

    // [N]:statfull]:mq - Create a list of users that should gain access to our ActiveMQ instance.
    List<User> userList = new ArrayList<>();
    userList.add(new User(
      username,
      password
    ));

    Network.NetworkOutputParameters networkOutputParameters = Network.getOutputParametersFromParameterStore(this, applicationEnvironment.getEnvironmentName());

    CfnSecurityGroup amqSecurityGroup = CfnSecurityGroup.Builder.create(this, "amqSecurityGroup")
      // [N] We supply the ID from our already existing VPC (retrieved from a previously deployed Network stack via the NetworkOutputParameters class). This will make sure our ActiveMQ instance resides within the same VPC as our main application.
      .vpcId(networkOutputParameters.getVpcId())
      .groupDescription("Security Group for the Amazon MQ instance")
      .groupName(applicationEnvironment.prefix("amqSecurityGroup"))
      .build();

    this.securityGroupId = amqSecurityGroup.getAttrGroupId();

    this.broker = CfnBroker.Builder
      .create(this, "amqBroker")
      .brokerName(applicationEnvironment.prefix("stratospheric-amq-message-broker"))
      .securityGroups(Collections.singletonList(this.securityGroupId))
      .subnetIds(Collections.singletonList(networkOutputParameters.getIsolatedSubnets().get(0)))
      // [N] To save consts, mq.t3.micro is the smallest & cheapest instance type.
      .hostInstanceType("mq.t3.micro")
      .engineType("ACTIVEMQ")
      .engineVersion("5.16.2")
      // [N] We choose the default SIMPLE (password-based) authentication strategy.
      .authenticationStrategy("SIMPLE")
      .encryptionOptions(
        CfnBroker.EncryptionOptionsProperty
          .builder()
          .useAwsOwnedKey(true)
          .build()
      )
      // [N] Pass the userList as the list of valid logins (containing exactly one user in our case) into the ActiveMQ construct to create access credentials.
      .users(userList)
      .publiclyAccessible(false)
      // [N] Amazon MQ will automatically take care of minor version upgrades.
      .autoMinorVersionUpgrade(true)
      // [N] Set the broker’s deployment mode to SINGLE_INSTANCE. This is the most simple setup possible, with just a single ActiveMQ instance running for our entire application.
      .deploymentMode("SINGLE_INSTANCE")
      .logs(CfnBroker.LogListProperty
        .builder()
        .general(true)
        .build())
      .build();

    createOutputParameters();
  }

  public static ActiveMqOutputParameters getOutputParametersFromParameterStore(Construct scope, ApplicationEnvironment applicationEnvironment) {
    return new ActiveMqOutputParameters(
      getParameterUsername(scope, applicationEnvironment),
      getParameterPassword(scope, applicationEnvironment),
      getParameterAmqpEndpoint(scope, applicationEnvironment),
      getParameterStompEndpoint(scope, applicationEnvironment),
      getParameterSecurityGroupId(scope, applicationEnvironment)
    );
  }

  private static String getParameterUsername(Construct scope, ApplicationEnvironment applicationEnvironment) {
    return StringParameter.fromStringParameterName(scope, PARAMETER_USERNAME, createParameterName(applicationEnvironment, PARAMETER_USERNAME))
      .getStringValue();
  }

  private static String getParameterPassword(Construct scope, ApplicationEnvironment applicationEnvironment) {
    return StringParameter.fromStringParameterName(scope, PARAMETER_PASSWORD, createParameterName(applicationEnvironment, PARAMETER_PASSWORD))
      .getStringValue();
  }

  private static String getParameterAmqpEndpoint(Construct scope, ApplicationEnvironment applicationEnvironment) {
    return StringParameter.fromStringParameterName(scope, PARAMETER_AMQP_ENDPOINT, createParameterName(applicationEnvironment, PARAMETER_AMQP_ENDPOINT))
      .getStringValue();
  }

  private static String getParameterStompEndpoint(Construct scope, ApplicationEnvironment applicationEnvironment) {
    return StringParameter.fromStringParameterName(scope, PARAMETER_STOMP_ENDPOINT, createParameterName(applicationEnvironment, PARAMETER_STOMP_ENDPOINT))
      .getStringValue();
  }

  private static String getParameterSecurityGroupId(Construct scope, ApplicationEnvironment applicationEnvironment) {
    return StringParameter.fromStringParameterName(scope, PARAMETER_SECURITY_GROUP_ID, createParameterName(applicationEnvironment, PARAMETER_SECURITY_GROUP_ID))
      .getStringValue();
  }

  /**
   * 
   * @return [N] a password consisting of 32 characteurs with at least 5 lower-cases, 5 upper-cases, & 5 digits.
   */
  private String generatePassword() {
    PasswordGenerator passwordGenerator = new PasswordGenerator();
    CharacterData lowerCaseChars = EnglishCharacterData.LowerCase;
    CharacterRule lowerCaseRule = new CharacterRule(lowerCaseChars);
    lowerCaseRule.setNumberOfCharacters(5);
    CharacterData upperCaseChars = EnglishCharacterData.UpperCase;
    CharacterRule upperCaseRule = new CharacterRule(upperCaseChars);
    upperCaseRule.setNumberOfCharacters(5);
    CharacterData digitChars = EnglishCharacterData.Digit;
    CharacterRule digitRule = new CharacterRule(digitChars);
    digitRule.setNumberOfCharacters(5);
    return passwordGenerator.generatePassword(32, lowerCaseRule, upperCaseRule, digitRule);
  }

  /**
   * Exports some output parameters, which we will later pass to our service stack for configuring our Spring Boot application to connect with the ActiveMQ instance.
   */
  private void createOutputParameters() {
    StringParameter.Builder.create(this, PARAMETER_USERNAME)
      .parameterName(createParameterName(applicationEnvironment, PARAMETER_USERNAME))
      .stringValue(username)
      .build();

    StringParameter.Builder.create(this, PARAMETER_PASSWORD)
      .parameterName(createParameterName(applicationEnvironment, PARAMETER_PASSWORD))
      .stringValue(password)
      .build();

    StringParameter.Builder.create(this, PARAMETER_AMQP_ENDPOINT)
      .parameterName(createParameterName(applicationEnvironment, PARAMETER_AMQP_ENDPOINT))
      .stringValue(Fn.select(0, this.broker.getAttrAmqpEndpoints()))
      .build();

    // [?] Not used
    String stompEndpointsFailoverString = "failover:(" + Fn.select(0, this.broker.getAttrStompEndpoints()) + "," + Fn.select(1, this.broker.getAttrStompEndpoints()) + ")";
    StringParameter.Builder.create(this, PARAMETER_STOMP_ENDPOINT)
      .parameterName(createParameterName(applicationEnvironment, PARAMETER_STOMP_ENDPOINT))
      .stringValue(Fn.select(0, this.broker.getAttrStompEndpoints()))
      .build();

    StringParameter.Builder.create(this, PARAMETER_SECURITY_GROUP_ID)
      .parameterName(createParameterName(applicationEnvironment, PARAMETER_SECURITY_GROUP_ID))
      .stringValue(this.securityGroupId)
      .build();
  }

  private static String createParameterName(ApplicationEnvironment applicationEnvironment, String parameterName) {
    return applicationEnvironment.getEnvironmentName() + "-" + applicationEnvironment.getApplicationName() + "-ActiveMq-" + parameterName;
  }

  public static class ActiveMqOutputParameters {
    private final String activeMqUsername;
    private final String activeMqPassword;
    private final String amqpEndpoint;
    private final String stompEndpoint;
    private final String activeMqSecurityGroupId;

    public ActiveMqOutputParameters(
      String activeMqUsername,
      String activeMqPassword,
      String amqpEndpoint,
      String stompEndpoint,
      String activeMqSecurityGroupId
    ) {
      this.activeMqUsername = activeMqUsername;
      this.activeMqPassword = activeMqPassword;
      this.amqpEndpoint = amqpEndpoint;
      this.stompEndpoint = stompEndpoint;
      this.activeMqSecurityGroupId = activeMqSecurityGroupId;
    }

    public String getAmqpEndpoint() {
      return amqpEndpoint;
    }

    public String getStompEndpoint() {
      return stompEndpoint;
    }

    public String getActiveMqUsername() {
      return activeMqUsername;
    }

    public String getActiveMqPassword() {
      return activeMqPassword;
    }

    public String getActiveMqSecurityGroupId() {
      return activeMqSecurityGroupId;
    }
  }

  static class User {

    String username;

    String password;

    public User() {
    }

    public User(String username, String password) {
      this.username = username;
      this.password = password;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }
}
