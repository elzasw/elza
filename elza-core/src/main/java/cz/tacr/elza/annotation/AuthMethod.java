package cz.tacr.elza.annotation;

import cz.tacr.elza.api.UsrPermission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotace pro oprávnění přístupu k metodě.
 *
 * @author Martin Šlapa
 * @since 27.04.2016
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface AuthMethod {

    UsrPermission.Permission[] permission();

}
