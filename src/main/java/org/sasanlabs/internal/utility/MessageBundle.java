package org.sasanlabs.internal.utility;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/** @author KSASAN preetkaran20@gmail.com */
@Component
public class MessageBundle {

    private MessageSource messageSource;

    public MessageBundle(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String getString(String code, Object[] args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}
