package cz.tacr.elza.deimport.context;

import java.io.Serializable;

public class SimpleIdHolder extends IdHolder {

    private final Class<?> entityClass;

    public SimpleIdHolder(Class<?> entityClass) {
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
