package cz.tacr.elza.configuration.hibernate.impl;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
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
        params.setProperty(TABLE_PARAM, "base_genid_nonblock");
        params.setProperty(VALUE_COLUMN_PARAM, "HodnotaId");
        params.setProperty(SEGMENT_COLUMN_PARAM, "NazevId");
        params.setProperty(SEGMENT_VALUE_PARAM, params.getProperty("target_table") + "|" + params.getProperty("target_column"));
        params.setProperty(INCREMENT_PARAM, "1");
        params.setProperty(SEGMENT_LENGTH_PARAM, "100");
        super.configure(type, params, dialect);
    }
}
