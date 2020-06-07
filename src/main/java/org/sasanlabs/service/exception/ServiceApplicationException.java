package org.sasanlabs.service.exception;

/** @author KSASAN preetkaran20@gmail.com */
public class ServiceApplicationException extends Exception {

    private static final long serialVersionUID = 1L;
    private ExceptionStatusCodeEnum exceptionStatusCode;
    private Object[] args;

    public ServiceApplicationException(
            ExceptionStatusCodeEnum exceptionStatusCodeEnum, Object... args) {
        this.exceptionStatusCode = exceptionStatusCodeEnum;
        this.setArgs(args);
    }

    public ServiceApplicationException(
            String message, ExceptionStatusCodeEnum exceptionStatusCodeEnum, Object... args) {
        super(message);
        this.exceptionStatusCode = exceptionStatusCodeEnum;
        this.setArgs(args);
    }

    public ServiceApplicationException(
            String message,
            Throwable throwable,
            ExceptionStatusCodeEnum exceptionStatusCodeEnum,
            Object... args) {
        super(message, throwable);
        this.exceptionStatusCode = exceptionStatusCodeEnum;
        this.setArgs(args);
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
