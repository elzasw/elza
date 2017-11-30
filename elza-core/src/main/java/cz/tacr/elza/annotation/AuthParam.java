package cz.tacr.elza.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotace pro oprávnění přístupu k parametru metody.
 *
 * @author Martin Šlapa
 * @since 27.04.2016
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.PARAMETER})
public @interface AuthParam {

    /**
     * Typy vstupního parametru
     */
    enum Type {
        FUND, FUND_VERSION, SCOPE, PARTY, REGISTRY, USER, GROUP
    }

    Type type();

}
