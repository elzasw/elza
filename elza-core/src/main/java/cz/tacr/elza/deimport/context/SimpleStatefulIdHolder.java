package cz.tacr.elza.deimport.context;

import java.io.Serializable;

public class SimpleStatefulIdHolder extends StatefulIdHolder {

    private final Class<?> entityClass;

    public SimpleStatefulIdHolder(Class<?> entityClass) {
        this.entityClass = entityClass;
    }

    /**
     * Instance will share same state which can be modified by this and source holder.
     */
    public SimpleStatefulIdHolder(Class<?> entityClass, StatefulIdHolder sourceHolder) {
        super(sourceHolder);
        this.entityClass = entityClass;
    }

    public void setId(Serializable id) {
        init(id);
    }

    @Override
    public void checkReferenceClass(Class<?> entityClass) {
        if (this.entityClass != entityClass) {
            throw new IllegalStateException(
                    "Class " + entityClass + " is not suitable as entity reference, expected:" + this.entityClass);
        }
    }
}
