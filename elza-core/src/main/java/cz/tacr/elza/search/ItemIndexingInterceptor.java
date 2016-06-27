package cz.tacr.elza.search;

import cz.tacr.elza.domain.ArrItem;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.interceptor.IndexingOverride;

import cz.tacr.elza.domain.ArrDescItem;


/**
 * Přdání interceptoru opravuje chybu ELZA-614.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 9. 2. 2016
 */
public class ItemIndexingInterceptor implements EntityIndexingInterceptor<ArrItem> {

    @Override
    public IndexingOverride onAdd(ArrItem arrDescItem) {
        return IndexingOverride.APPLY_DEFAULT;
    }

    @Override
    public IndexingOverride onUpdate(ArrItem arrDescItem) {
        return IndexingOverride.APPLY_DEFAULT;
    }

    @Override
    public IndexingOverride onDelete(ArrItem arrDescItem) {
        return IndexingOverride.APPLY_DEFAULT;
    }

    @Override
    public IndexingOverride onCollectionUpdate(ArrItem arrDescItem) {
        return onUpdate(arrDescItem);
    }
}
