// [N]:websocket]:stomp]:statefull - File responsible for exchanging messages with our WebSocket server.

let stompClient = null;

/**
 * Creates a STOMP client via the SockJS library.<p/>
 * [N]:stomp]:websocket - Note, if WebSocket isn’t supported by the browser, SockJS will degrade gracefully to HTTP. 
 * @param email An itentifier of the user to which is targetted the message.
 */ 
function connectToWebSocketEndpoint(email) {
  const socket = new SockJS('/websocket');

  stompClient = Stomp.over(socket);
  stompClient.connect({}, () => {
    // [?] From where comes the message
    // [N] Subscribe to a generic channel
    stompClient.subscribe('/topic/todoUpdates', function (message) {
      $('#message').html(message.body);
      $('#toast').toast('show');
    });

    if (email) {
      // [N] Subscribe to a channel specific to the authenticated user (email)
      stompClient.subscribe('/topic/todoUpdates/' + email, function (message) {
        $('#message').html(message.body);
        $('#toast').toast('show');
      });
    }
  });
}

function disconnectFromWebSocketEndpoint() {
  if (stompClient !== null) {
    stompClient.disconnect();
  }
}

// Toast a message for 10 secs (see https://getbootstrap.com/docs/4.3/components/toasts/).
$(document).ready(function () {
  $('#toast').toast({delay: 10000});
});
