package cz.tacr.elza.search;

import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.interceptor.IndexingOverride;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.utils.ProxyUtils;

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

        ArrItem item = ProxyUtils.deproxy(arrData.getItem());
        if (item instanceof ArrOutputItem) {
            return IndexingOverride.SKIP;
        }
        return IndexingOverride.APPLY_DEFAULT;
    }

    @Override
    public IndexingOverride onUpdate(final ArrData arrData) {
        if (arrData.getItem() == null) {
            return IndexingOverride.REMOVE;
        }

        ArrItem item = ProxyUtils.deproxy(arrData.getItem());
        if (item instanceof ArrItem) {
            return IndexingOverride.REMOVE;
        }
        return IndexingOverride.UPDATE;
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
