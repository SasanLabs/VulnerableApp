package org.sasanlabs.controller.exception;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sasanlabs.internal.utility.MessageBundle;
import org.sasanlabs.service.exception.ExceptionStatusCodeEnum;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Exception Handler for all the VulnerableApp's exceptions.
 *
 * @author KSASAN preetkaran20@gmail.com
 */
@ControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {

    private MessageBundle messageBundle;

    private static final transient Logger LOGGER =
            LogManager.getLogger(ControllerExceptionHandler.class);

    public ControllerExceptionHandler(MessageBundle messageBundle) {
        this.messageBundle = messageBundle;
    }

    @ExceptionHandler(ControllerException.class)
    public ResponseEntity<String> handleControllerExceptions(
            ControllerException ex, WebRequest request) {
        LOGGER.error("Controller Exception Occurred :-", ex);
        return new ResponseEntity<String>(
                ex.getExceptionStatusCode().getMessage(ex.getArgs(), messageBundle),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleExceptions(Exception ex, WebRequest request) {
        LOGGER.error("General Exception Occurred :- ", ex);
        return new ResponseEntity<String>(
                ExceptionStatusCodeEnum.SYSTEM_ERROR.getMessage(null, messageBundle),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
