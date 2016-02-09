package cz.tacr.elza.search;

import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.interceptor.IndexingOverride;

import cz.tacr.elza.domain.ArrDescItem;


/**
 * Přdání interceptoru opravuje chybu ELZA-614.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 9. 2. 2016
 */
public class DescItemIndexingInterceptor implements EntityIndexingInterceptor<ArrDescItem> {

    @Override
    public IndexingOverride onAdd(ArrDescItem arrDescItem) {
        return IndexingOverride.APPLY_DEFAULT;
    }

    @Override
    public IndexingOverride onUpdate(ArrDescItem arrDescItem) {
        return IndexingOverride.APPLY_DEFAULT;
    }

    @Override
    public IndexingOverride onDelete(ArrDescItem arrDescItem) {
        return IndexingOverride.APPLY_DEFAULT;
    }

    @Override
    public IndexingOverride onCollectionUpdate(ArrDescItem arrDescItem) {
        return onUpdate(arrDescItem);
    }
}
