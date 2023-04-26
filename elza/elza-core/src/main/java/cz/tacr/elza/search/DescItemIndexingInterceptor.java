package cz.tacr.elza.search;

import cz.tacr.elza.domain.ArrDescItem;
//import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor; TODO hibernate search 6
//import org.hibernate.search.indexes.interceptor.IndexingOverride;

import cz.tacr.elza.domain.ArrItem;


/**
 * Přdání interceptoru opravuje chybu ELZA-614.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 9. 2. 2016
 */
//public class DescItemIndexingInterceptor implements EntityIndexingInterceptor<ArrItem> { TODO hibernate search 6
//
//    @Override
//    public IndexingOverride onAdd(final ArrItem arrItem) {
//        return IndexingOverride.APPLY_DEFAULT;
//    }
//
//    @Override
//    public IndexingOverride onUpdate(final ArrItem arrItem) {
//        return IndexingOverride.APPLY_DEFAULT;
//    }
//
//    @Override
//    public IndexingOverride onDelete(final ArrItem arrItem) {
//        return IndexingOverride.APPLY_DEFAULT;
//    }
//
//    @Override
//    public IndexingOverride onCollectionUpdate(final ArrItem arrItem) {
//        return onUpdate(arrItem);
//    }
//}
