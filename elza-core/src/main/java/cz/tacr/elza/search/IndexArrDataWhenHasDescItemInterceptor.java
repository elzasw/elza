package cz.tacr.elza.search;

import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.interceptor.IndexingOverride;

import cz.tacr.elza.domain.ArrData;

/**
 * Indexuje {@link ArrData} jen pokud patří k nějakému atributu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 20. 1. 2016
 */
public class IndexArrDataWhenHasDescItemInterceptor implements EntityIndexingInterceptor<ArrData> {

    @Override
    public IndexingOverride onAdd(ArrData arrData) {
        if (arrData.getItem() == null) {
            return IndexingOverride.SKIP;
        }

        return IndexingOverride.APPLY_DEFAULT;
    }

    @Override
    public IndexingOverride onUpdate(ArrData arrData) {
        if (arrData.getItem() == null) {
            return IndexingOverride.REMOVE;
        }

        return IndexingOverride.UPDATE;
    }

    @Override
    public IndexingOverride onDelete(ArrData arrData) {
        return IndexingOverride.APPLY_DEFAULT;
    }

    @Override
    public IndexingOverride onCollectionUpdate(ArrData arrData) {
        return onUpdate(arrData);
    }
}
