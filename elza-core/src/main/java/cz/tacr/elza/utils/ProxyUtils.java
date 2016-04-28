package cz.tacr.elza.utils;

import org.hibernate.proxy.HibernateProxy;

/**
 * Pomocná třída pro práci s objekty s HibernateProxy.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 28. 4. 2016
 */
public class ProxyUtils {

    /**
     * Odstraní proxy z do objektu.
     *
     * @param object objekt s proxy
     *
     * @return objekt bez proxy
     */
    @SuppressWarnings("unchecked")
    public static <T> T deproxy(final Object object) {
        T deproxied;
        if (object instanceof HibernateProxy) {
            HibernateProxy proxy = (HibernateProxy) object;

            deproxied = (T) proxy.getHibernateLazyInitializer().getImplementation();
        } else {
            deproxied = (T) object;
        }

        return deproxied;
    }
}
