package org.sasanlabs.internal.utility.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

class VulnerableAppWebSocketHandlerTest {

    public static class Controller {
        public String echo(WebSocketSession session, String message) {
            return "echo:" + message;
        }

        public String silent(WebSocketSession session, String message) {
            return null;
        }

        public String failing(WebSocketSession session, String message) throws IOException {
            throw new IOException("boom");
        }

        public String crashing(WebSocketSession session, String message) {
            throw new AssertionError("crash");
        }
    }

    private VulnerableAppWebSocketHandler handlerFor(String methodName)
            throws NoSuchMethodException {
        Method method =
                Controller.class.getMethod(methodName, WebSocketSession.class, String.class);
        return new VulnerableAppWebSocketHandler(new Controller(), method);
    }

    private WebSocketSession openSession() {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    @Test
    void handleTextMessage_sendsTheReturnedTextBackToTheClient() throws Exception {
        WebSocketSession session = openSession();

        handlerFor("echo").handleTextMessage(session, new TextMessage("hi"));

        ArgumentCaptor<WebSocketMessage<?>> sent = ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(session).sendMessage(sent.capture());
        assertThat(sent.getValue().getPayload()).isEqualTo("echo:hi");
    }

    @Test
    void handleTextMessage_sendsNothingWhenTheMethodReturnsNull() throws Exception {
        WebSocketSession session = openSession();

        handlerFor("silent").handleTextMessage(session, new TextMessage("hi"));

        verify(session, never()).sendMessage(any());
    }

    @Test
    void handleTextMessage_sendsNothingWhenTheSessionIsAlreadyClosed() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(false);

        handlerFor("echo").handleTextMessage(session, new TextMessage("hi"));

        verify(session, never()).sendMessage(any());
    }

    @Test
    void handleTextMessage_rethrowsTheExceptionOfTheMethodItself() {
        assertThatThrownBy(
                        () ->
                                handlerFor("failing")
                                        .handleTextMessage(openSession(), new TextMessage("hi")))
                .isInstanceOf(IOException.class)
                .hasMessage("boom");
    }

    @Test
    void handleTextMessage_doesNotUnwrapAnErrorThrownByTheMethod() {
        assertThatThrownBy(
                        () ->
                                handlerFor("crashing")
                                        .handleTextMessage(openSession(), new TextMessage("hi")))
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(AssertionError.class);
    }
}
