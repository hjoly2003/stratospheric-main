package dev.stratospheric.todoapp.config;

import io.netty.resolver.DefaultAddressResolverGroup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompReactorNettyCodec;
import org.springframework.messaging.tcp.reactor.ReactorNettyTcpClient;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * [N]:mq]:websocket]:relay - Spring configuration for the web socket<p/>
 * Upon startup, our Spring Boot application will now open up a connection to our <em>ActiveMQ</em> instance and act as a WebSocket relay under the path prefix "/topic".<p/>
 * The {@code @EnableWebSocketMessageBroker} annotation enables WebSocket communication.<p/> 
 * It implements the {@code WebSocketMessageBrokerConfigurer} interface, which provides us with the {@code configureMessageBroker()} and {@code registerStompEndpoints()} methods. These methods allow us to configure the connection to our <em>ActiveMQ</em> message broker.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final Endpoint messageBrokerEndpoint;
  private final String messageBrokerUser;
  private final String messageBrokerPassword;
  private final boolean messageBrokerUseSsl;

  /**
   * {@code websocketRelayEndpoint}, {@code websocketUsername}, and {@code websocketPassword} arguments are taken from the {@code ActiveMqStack}’s output parameters that we made available to the application via environment variables.
   * @param websocketRelayEndpoint
   * @param messageBrokerUser
   * @param messageBrokerPassword
   * @param messageBrokerUseSsl
   * @see application-aws.yml
   */
  public WebSocketConfig(
    @Value("${spring.activemq.broker-url}") String websocketRelayEndpoint,
    @Value("${spring.activemq.user}") String messageBrokerUser,
    @Value("${spring.activemq.password}") String messageBrokerPassword,
    @Value("${custom.web-socket-relay-use-ssl:#{false}}") boolean messageBrokerUseSsl
  ) {
    this.messageBrokerEndpoint = Endpoint.fromEndpointString(websocketRelayEndpoint);
    this.messageBrokerUser = messageBrokerUser;
    this.messageBrokerPassword = messageBrokerPassword;
    this.messageBrokerUseSsl = messageBrokerUseSsl;
  }

  @Override
  public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {
    // [N]:mq]:relay - The  custom TCP client then is used by calling setTcpClient(tcpClient) when we register our STOMP relay.
    ReactorNettyTcpClient<byte[]> customTcpClient = this.messageBrokerUseSsl ?
      getCustomTcpClientWithSSLSupport() : getCustomTcpClientWithoutSSLSupport();

    registry
      .enableStompBrokerRelay("/topic")
      .setAutoStartup(true)
      .setClientLogin(this.messageBrokerUser)
      .setClientPasscode(this.messageBrokerPassword)
      .setSystemLogin(this.messageBrokerUser)
      .setSystemPasscode(this.messageBrokerPassword)
      .setTcpClient(customTcpClient);
  }

  private ReactorNettyTcpClient<byte[]> getCustomTcpClientWithoutSSLSupport() {
    return new ReactorNettyTcpClient<>(configurer -> configurer
      .host(this.messageBrokerEndpoint.host)
      .port(this.messageBrokerEndpoint.port), new StompReactorNettyCodec());
  }

  /**
   * Returns a TCP client for connecting to the <em>Amazon MQ</em> instance.<p/>
   * Since <em>Amazon MQ</em> instances are only - and rightfully so - available via connections encrypted through SSL as well as domain names rather than IP addresses (which also is pretty much a prerequisite for using SSL) we have to resort to creating our TCP client based on {@code Reactor}’s {@code ReactorNettyTcpClient}.
   * @see <a href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/messaging/tcp/reactor/ReactorNettyTcpClient.html">ReactorNettyTcpClient</a>
   */
  private ReactorNettyTcpClient<byte[]> getCustomTcpClientWithSSLSupport() { 
    return new ReactorNettyTcpClient<>(
      configurer 
        -> configurer
            .host(this.messageBrokerEndpoint.host)
            .port(this.messageBrokerEndpoint.port)
            // [N]:mq - "resolver(...)" provides us with the working DNS resolver that allows us to resolve domain names in our VPC.
            .resolver(DefaultAddressResolverGroup.INSTANCE)
            // [N]:mq - "secure()" enables SSL connectivity depending on the value of the member messageBrokerUseSsl, which is controlled by the web-socket-relay-use-ssl property in application.yml configuration file.
            .secure(), 
      new StompReactorNettyCodec()
    );
  }

  /**
   * [N]:mq]:relay - Returns a round-robin TCP client.<p/>
   * Although we’re only running a single ActiveMQ instance rather than an active/standby set of instances, the custom TCP client would also support multiple hosts in a round-robin fashion
   * @param endpoint
   */
  private ReactorNettyTcpClient<byte[]> createRoundRobinTcpClient(Endpoint endpoint) {
    final List<InetSocketAddress> addressList = new ArrayList<>();

    for (String hostURI : endpoint.activeStandbyHosts) {
      String[] hostAndPort = hostURI.split(":");
      addressList.add(new InetSocketAddress(hostAndPort[0], Integer.parseInt(hostAndPort[1])));
    }

    final RoundRobinList<InetSocketAddress> addresses = new RoundRobinList<>(addressList);

    return new ReactorNettyTcpClient<>(
      builder 
        -> builder
            .remoteAddress(addresses::get)
            .secure()
            .resolver(DefaultAddressResolverGroup.INSTANCE),
      new StompReactorNettyCodec()
    );
  }

  /**
   * [N]:stomp - Add a "/websocket" endpoint with SockJS195 support enabled.<p/>
   * <a href="https://github.com/sockjs">SockJS</a> is a library for WebSocket connectivity and emulation that serves as a fallback option for when WebSocket support isn’t available on either the client or the server.
   * @param registry Stomp endpoint registry
   */
  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
      .addEndpoint("/websocket")
      .withSockJS();
  }

  /**
   * An endpoint defined via either:<ul>
   *  <li>a combination of {@code host} and {@code port} (from "stomp+ssl://{host}:{port}") or </li>
   *  <li>the {@code activeStandbyHost} list (from "failover:(//{host}[,{host},...]")</li>
   * </ul>
   */
  private static class Endpoint {
    final String host;
    final Integer port;
    final List<String> activeStandbyHosts;

    public Endpoint(String host, int port) {
      this.host = host;
      this.port = port;
      this.activeStandbyHosts = null;
    }

    public Endpoint(List<String> activeStandbyHosts) {
      this.host = null;
      this.port = null;
      this.activeStandbyHosts = activeStandbyHosts;
    }

    /**
     * Builds an {@code Endpoint} instance from the given endpoint string (from either "stomp+ssl://{host}:{port}" or "failover:(//{host}[,{host},...])")
     * @param endpoint String containing the "stomp+ssl://" protocol
     * @return the {@code Endpoint}  Object as built from the given endpoint stripped of its "stomp+ssl://" protocol (since the {@code ReactorNettiTcpClient} expects only the host and port as parameters).
     */
    static Endpoint fromEndpointString(String endpoint) {
      if (endpoint == null) {
        return null;
      }

      String host;
      String port;

      Pattern hostAndPortPattern = Pattern.compile("^(.*):([0-9]+$)");
      Matcher hostAndPortMatcher = hostAndPortPattern.matcher(endpoint);

      if (hostAndPortMatcher.matches()) {
        host = hostAndPortMatcher
          .group(1)
          .replace("stomp+ssl://", ""); // see https://stackoverflow.com/questions/49964647/spring-websockets-activemq-convertandsendtouser
        port = hostAndPortMatcher.group(2);

        return new Endpoint(host, Integer.parseInt(port));
      }

      Pattern failoverURIPattern = Pattern.compile("^(failover:\\(.*\\))");
      Matcher failoverURIMatcher = failoverURIPattern.matcher(endpoint);
      if (failoverURIMatcher.matches()) {
        Pattern hostPattern = Pattern.compile("//(.+?)[,)]{1}");
        Matcher hostMatcher = hostPattern.matcher(endpoint);
        List<String> activeStandbyHosts = new ArrayList<>();
        while (hostMatcher.find()) {
          activeStandbyHosts.add(hostMatcher.group(1));
        }

        return new Endpoint(activeStandbyHosts);
      }

      if (!(hostAndPortMatcher.matches() || failoverURIMatcher.matches())) {
        throw new IllegalStateException(String.format("Invalid endpoint string (must either consist of hostname and port or a failover URI): %s", endpoint));
      }

      return null;
    }
  }

  private static class RoundRobinList<T> {

    private Iterator<T> iterator;
    private final Collection<T> elements;

    public RoundRobinList(Collection<T> elements) {
      this.elements = elements;
      iterator = this.elements.iterator();
    }

    public synchronized T get() {
      if (iterator.hasNext()) {
        return iterator.next();
      } else {
        iterator = elements.iterator();
        return iterator.next();
      }
    }

    public int size() {
      return elements.size();
    }
  }
}
