package cz.tacr.elza.filter.condition;

import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.QueryBuilder;

import cz.tacr.elza.domain.ArrDescItem;

/**
 * Má hodnotu s typem.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 20. 12. 2016
 */
public class NotEmptyPacketTypeDescItemCondition implements LuceneDescItemCondition {

    @Override
    public Query createLuceneQuery(final QueryBuilder queryBuilder) {
        return queryBuilder.
                range().
		        onField(ArrDescItem.SPECIFICATION_ATT).
                from(Integer.MIN_VALUE).
                to(Integer.MAX_VALUE).
                createQuery();
    }
}
