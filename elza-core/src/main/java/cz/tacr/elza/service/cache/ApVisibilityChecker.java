package cz.tacr.elza.service.cache;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ApVisibilityChecker extends VisibilityChecker.Std {

    /**
     * Seznam tříd, které se můžou serializovat.
     */
    private final Set<Class<?>> classes;

    public ApVisibilityChecker(final Class<?>... clazzes) {
        super(JsonAutoDetect.Visibility.PUBLIC_ONLY);
        classes = new HashSet<>();
        Collections.addAll(classes, clazzes);
    }

    @Override
    public boolean isGetterVisible(Method m) {
        for (Class<?> aClass1 : classes) {
            if (aClass1.isAssignableFrom(m.getReturnType())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isGetterVisible(AnnotatedMethod m) {
        return isGetterVisible(m.getAnnotated());
    }

}
