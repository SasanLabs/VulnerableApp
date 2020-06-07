package org.sasanlabs.controller.exception;

import org.sasanlabs.service.exception.ExceptionStatusCodeEnum;
import org.sasanlabs.service.exception.ServiceApplicationException;

/** @author KSASAN preetkaran20@gmail.com */
public class ControllerException extends Exception {

    private static final long serialVersionUID = 1L;
    private ExceptionStatusCodeEnum exceptionStatusCode;
    private Object[] args;

    public ControllerException(
            String message,
            Throwable throwable,
            ExceptionStatusCodeEnum exceptionStatusCodeEnum,
            Object[] args) {
        super(message, throwable);
        this.exceptionStatusCode = exceptionStatusCodeEnum;
        this.args = args;
    }

    public ControllerException(ServiceApplicationException serviceApplicationException) {
        super(serviceApplicationException);
        exceptionStatusCode = serviceApplicationException.getExceptionStatusCode();
        args = serviceApplicationException.getArgs();
    }

    public ExceptionStatusCodeEnum getExceptionStatusCode() {
        return exceptionStatusCode;
    }

    public void setExceptionStatusCode(ExceptionStatusCodeEnum exceptionStatusCode) {
        this.exceptionStatusCode = exceptionStatusCode;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }
}
