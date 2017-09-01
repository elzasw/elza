package cz.tacr.elza.search;

import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.interceptor.IndexingOverride;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.utils.HibernateUtils;

/**
 * Indexuje {@link ArrData} jen pokud patří k nějakému atributu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 20. 1. 2016
 */
public class IndexArrDataWhenHasDescItemInterceptor implements EntityIndexingInterceptor<ArrData> {

    @Override
    public IndexingOverride onAdd(final ArrData arrData) {
        if (arrData.getItem() == null) {
            return IndexingOverride.SKIP;
        }

        Class<?> itemClass = HibernateUtils.getPersistentClass(arrData.getItem());
        if (ArrDescItem.class.isAssignableFrom(itemClass)) {
            return IndexingOverride.APPLY_DEFAULT;
        }
        return IndexingOverride.SKIP;
    }

    @Override
    public IndexingOverride onUpdate(final ArrData arrData) {
        if (arrData.getItem() == null) {
            return IndexingOverride.SKIP;
        }

        Class<?> itemClass = HibernateUtils.getPersistentClass(arrData.getItem());
        if (ArrDescItem.class.isAssignableFrom(itemClass)) {
            return IndexingOverride.APPLY_DEFAULT;
        }
        return IndexingOverride.SKIP;
    }

    @Override
    public IndexingOverride onDelete(final ArrData arrData) {
        return IndexingOverride.APPLY_DEFAULT;
    }

    @Override
    public IndexingOverride onCollectionUpdate(final ArrData arrData) {
        return onUpdate(arrData);
    }
}
