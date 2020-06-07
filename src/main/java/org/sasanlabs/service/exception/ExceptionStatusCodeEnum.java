package org.sasanlabs.service.exception;

import org.sasanlabs.internal.utility.MessageBundle;

/** @author KSASAN preetkaran20@gmail.com */
public enum ExceptionStatusCodeEnum {
    INVALID_END_POINT("INVALID_END_POINT"),
    INVALID_LEVEL("INVALID_LEVEL"),
    UNAVAILABLE_LEVEL("UNAVAILABLE_LEVEL"),
    INVALID_ACCESS("INVALID_ACCESS"),
    INVALID_ARGUMENTS("INVALID_AGRUMENTS"),
    SYSTEM_ERROR("SYSTEM_ERROR");

    private String label;

    private ExceptionStatusCodeEnum(String label) {
        this.label = label;
    }

    public String getMessage(Object[] args, MessageBundle messageBundle) {
        return messageBundle.getString(this.label, args);
    }
}
