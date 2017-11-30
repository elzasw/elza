package cz.tacr.elza.hibernate;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.service.ServiceRegistry;
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

    public static final String DEFAULT_INCREMENT_SIZE = "20";

    @Override
    public void configure(final Type type, final Properties params, final ServiceRegistry serviceRegistry) throws MappingException {
        params.setProperty(TABLE_PARAM, "db_hibernate_sequences");
        params.setProperty(SEGMENT_VALUE_PARAM, params.getProperty("target_table") + "|" + params.getProperty("target_column"));
        params.setProperty(INCREMENT_PARAM, DEFAULT_INCREMENT_SIZE);
        //params.setProperty(OPT_PARAM, "pooled");
        super.configure(type, params, serviceRegistry);
    }

    @Override
    public Serializable generate(final SharedSessionContractImplementor session, final Object obj) {
        Serializable id = getReplicatedId(obj);
        if (id == null) {
            id = super.generate(session, obj);
        }
        return id;
    }

    private Serializable getReplicatedId(final Object obj) {
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
