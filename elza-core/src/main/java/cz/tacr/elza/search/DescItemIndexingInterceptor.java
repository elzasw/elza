package cz.tacr.elza.search;

import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.interceptor.IndexingOverride;

import cz.tacr.elza.domain.ArrItem;


/**
 * Přdání interceptoru opravuje chybu ELZA-614.
 *
 * @since 9. 2. 2016
 */
public class DescItemIndexingInterceptor implements EntityIndexingInterceptor<ArrItem> {

    @Override
    public IndexingOverride onAdd(final ArrItem arrItem) {
        // if item is deleted it will be removed from index
        if (arrItem.getDeleteChangeId() != null ||
                arrItem.getDeleteChange() != null) {
            return IndexingOverride.REMOVE;
        }
        return IndexingOverride.APPLY_DEFAULT;
    }

    @Override
    public IndexingOverride onUpdate(final ArrItem arrItem) {
        // if item is deleted it will be removed from index
        if (arrItem.getDeleteChangeId() != null ||
                arrItem.getDeleteChange() != null) {
            return IndexingOverride.REMOVE;
        }
        return IndexingOverride.APPLY_DEFAULT;
    }

    @Override
    public IndexingOverride onDelete(final ArrItem arrItem) {
        return IndexingOverride.APPLY_DEFAULT;
    }

    @Override
    public IndexingOverride onCollectionUpdate(final ArrItem arrItem) {
        return onUpdate(arrItem);
    }
}
