package cz.tacr.elza.dataexchange.input.context;

import org.hibernate.Session;

public interface EntityIdHolder<E> {

    Integer getEntityId();

    /**
     * Return the persistent instance with the given identifier, assuming that the
     * instance exists. This method might return a proxied instance.
     */
    E getEntityRef(Session session);
}