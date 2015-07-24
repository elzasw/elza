package cz.tacr.elza.configuration.hibernate.impl;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.id.enhanced.StandardOptimizerDescriptor;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.type.Type;

import java.util.Properties;


/**
 * Generátor ID pro hibernate podle specifikace.
 *
 * <p>
 *     Pro aktivaci je nutno použít {@link IdentifierGeneratorStrategyProvider}.
 * </p>
 *
 * @author <a href="mailto:jaroslav.pubal@marbes.cz">Jaroslav Půbal</a>
 */
public class TableIdGenerator extends TableGenerator {

    @Override
    public void configure(Type type, Properties params, Dialect dialect) throws MappingException {
//        params.setProperty(TABLE_PARAM, "hibernate_sequences");
        params.setProperty(INCREMENT_PARAM, "1");
        super.configure(type, params, dialect);
    }
}
