package cz.tacr.elza.dataexchange.output.writer.xml;

import cz.tacr.elza.schema.v2.ObjectFactory;

public class EdxOutputHelper {
    static public final ObjectFactory objectFactory = new ObjectFactory();

    public static ObjectFactory getObjectFactory() {
        return objectFactory;
    }
}
