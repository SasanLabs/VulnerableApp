package org.sasanlabs.sast;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class VulnerableLineLoggingWrapper {

    public <T> T log(T instance) {
        return instance;
    }

    public <R> R log(Supplier<R> supplier) {
        return supplier.get();
    }
}
