package cz.tacr.elza.core.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cz.tacr.elza.domain.UsrPermission;

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
