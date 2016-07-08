package cz.tacr.elza.configuration.hibernate;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;


/**
 * Výchozí konfigurace hibernate.
 *
 *
 * @author <a href="mailto:jaroslav.pubal@marbes.cz">Jaroslav Půbal</a>
 */
@Configuration
@PropertySource("classpath:/cz/tacr/elza/configuration/hibernate/hibernate.properties")
public class HibernateConfiguration {


}
