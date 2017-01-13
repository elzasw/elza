package cz.tacr.elza.search;

import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.interceptor.IndexingOverride;

import cz.tacr.elza.domain.ArrItem;


/**
 * Přdání interceptoru opravuje chybu ELZA-614.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 9. 2. 2016
 */
public class ItemIndexingInterceptor implements EntityIndexingInterceptor<ArrItem> {

    @Override
    public IndexingOverride onAdd(final ArrItem arrDescItem) {
        return IndexingOverride.APPLY_DEFAULT;
    }

    @Override
    public IndexingOverride onUpdate(final ArrItem arrDescItem) {
        return IndexingOverride.APPLY_DEFAULT;
    }

    @Override
    public IndexingOverride onDelete(final ArrItem arrDescItem) {
        return IndexingOverride.APPLY_DEFAULT;
    }

    @Override
    public IndexingOverride onCollectionUpdate(final ArrItem arrDescItem) {
        return onUpdate(arrDescItem);
    }
}
