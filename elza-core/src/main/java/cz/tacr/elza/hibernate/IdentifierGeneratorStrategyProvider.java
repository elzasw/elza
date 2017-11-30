package cz.tacr.elza.hibernate;

import java.util.HashMap;
import java.util.Map;


/**
 * Přetížení volby generátoru ID.
 *
 * Hibernate vyžaduje u každé tabulky uvedení způsobu generování ID.
 * Náš cíl je globálně definovat generátor a jeho konfiguraci na jednom místě.
 * V entitách chceme mít pouze @GeneratedValue bez dalších parametrů.
 * <p>
 *     Musíme nastavit aby se používaly enhaced generátory, protože ty původní "native" nelze přetížit.
 *     Potom se nastaví náše faktory pro generátory.
 *
 *     #viz org.hibernate.cfg.AnnotationBinder.generatorType
 *     spring.jpa.properties.hibernate.id.new_generator_mappings=true
 *     #viz org.hibernate.jpa.AvailableSettings.IDENTIFIER_GENERATOR_STRATEGY_PROVIDER
 *     spring.jpa.properties.hibernate.ejb.identifier_generator_strategy_provider=cz.tacr.elza.configuration.hibernate.impl.IdentifierGeneratorStrategyProvider
 * </p>
 *
 * @author <a href="mailto:jaroslav.pubal@marbes.cz">Jaroslav Půbal</a>
 */
public class IdentifierGeneratorStrategyProvider implements org.hibernate.jpa.spi.IdentifierGeneratorStrategyProvider  {

    @Override
    public Map<String, Class<?>> getStrategies() {
        Map<String, Class<?>> map = new HashMap<String, Class<?>>();
        //viz org.hibernate.cfg.AnnotationBinder.generatorType()
        //pro defaultni javax.persistence.GenerationType.AUTO zaregistruje náš IdentifierGenerator
        map.put(org.hibernate.id.enhanced.SequenceStyleGenerator.class.getName(), TableIdGenerator.class);
        return map;
    }
}
