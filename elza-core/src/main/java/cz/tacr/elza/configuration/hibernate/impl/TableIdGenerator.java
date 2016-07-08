package cz.tacr.elza.configuration.hibernate.impl;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.type.Type;


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
        params.setProperty(TABLE_PARAM, "db_hibernate_sequences");
        params.setProperty(SEGMENT_VALUE_PARAM, params.getProperty("target_table") + "|" + params.getProperty("target_column"));
        params.setProperty(INCREMENT_PARAM, "1");
        //params.setProperty(OPT_PARAM, "pooled");
        super.configure(type, params, dialect);
    }

    @Override
    public Serializable generate(SessionImplementor session, Object obj) {
        Serializable id = getReplicatedId(obj);
        if (id == null) {
            id = super.generate(session, obj);
        }
        return id;
    }

    private Serializable getReplicatedId(Object obj) {
        try {
            Class<?> cls = obj.getClass();

            ArrayList<Field> fields = new ArrayList<>();
            fields.addAll(Arrays.asList(cls.getDeclaredFields()));
            if (cls.getSuperclass() != null) {
                fields.addAll(Arrays.asList(cls.getSuperclass().getDeclaredFields()));
            }
            for (Field field : fields) {
                if (field.getAnnotation(Id.class) != null && field.getAnnotation(GeneratedValue.class) != null) {
                    field.setAccessible(true);
                    Serializable id = (Serializable) field.get(obj);
                    if (id != null) {
                        return id;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
