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
import org.hibernate.id.enhanced.StandardOptimizerDescriptor;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.type.Type;


/**
 * Generátor ID využívající optimalizaci HiLo (rezervuje si sadu id, kterou v paměti přiděluje. Až ji vyčerpá,
 * rezervuje
 * další sadu).
 *
 * Počet rezervovaných ID: {@link #DEFAULT_INCREMENT_SIZE}.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 15.01.2016
 */
public class TableIdHiLoGenerator extends TableGenerator {

    /**
     * Po každých dvaceti hodnotách se zvýší počítadlo v tabulce sekvencí.
     */
    public static final String DEFAULT_INCREMENT_SIZE = "1";

    @Override
    public void configure(final Type type, final Properties params, final Dialect d) throws MappingException {
        params.setProperty(TABLE_PARAM, "db_hibernate_sequences");
        params.setProperty(OPT_PARAM, StandardOptimizerDescriptor.HILO.getExternalName());
        params.setProperty(SEGMENT_VALUE_PARAM,
                params.getProperty("target_table") + "|" + params.getProperty("target_column"));
        params.setProperty(INCREMENT_PARAM, DEFAULT_INCREMENT_SIZE);
        params.setProperty(OPT_PARAM, StandardOptimizerDescriptor.HILO.getExternalName());

        super.configure(type, params, d);
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
