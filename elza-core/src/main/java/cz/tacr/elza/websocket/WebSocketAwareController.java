package cz.tacr.elza.websocket;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Informační rozhraní pro kontrolery, které zpracovávají websocket - slouží jen jako informativní.
 *
 * @author Jaroslav Todt [jaroslav.todt@lightcomp.cz]
 * @since 28.8.2016
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface WebSocketAwareController {

}
