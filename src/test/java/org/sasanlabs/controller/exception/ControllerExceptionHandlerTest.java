package org.sasanlabs.controller.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sasanlabs.internal.utility.MessageBundle;
import org.sasanlabs.service.exception.ExceptionStatusCodeEnum;
import org.sasanlabs.service.exception.ServiceApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

@ExtendWith(MockitoExtension.class)
class ControllerExceptionHandlerTest {

    @Mock private MessageBundle messageBundle;

    @Mock private WebRequest webRequest;

    @InjectMocks private ControllerExceptionHandler controllerExceptionHandler;

    @Test
    void shouldHandleControllerExceptions() {
        // Arrange
        when(messageBundle.getString(any(), any())).thenReturn("Exception occurred");
        ServiceApplicationException serviceApplicationException =
                new ServiceApplicationException(
                        ExceptionStatusCodeEnum.SYSTEM_ERROR, new NullPointerException("ex"));

        // Act
        ResponseEntity<String> responseEntity =
                controllerExceptionHandler.handleControllerExceptions(
                        new ControllerException(serviceApplicationException), webRequest);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals("Exception occurred", responseEntity.getBody());
        verify(messageBundle).getString(any(), any());
    }

    @Test
    void shouldHandleExceptions() {
        // Arrange
        when(messageBundle.getString(any(), any())).thenReturn("IO exception occurred");

        // Act
        ResponseEntity<String> responseEntity =
                controllerExceptionHandler.handleExceptions(
                        new IOException("IO operation failed"), webRequest);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals("IO exception occurred", responseEntity.getBody());
        verify(messageBundle).getString(any(), any());
    }
}
