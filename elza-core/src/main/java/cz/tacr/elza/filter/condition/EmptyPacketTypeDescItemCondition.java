package cz.tacr.elza.filter.condition;

import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.util.Assert;

/**
 * Má hodnotu bez typu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 */
public class EmptyPacketTypeDescItemCondition implements LuceneDescItemCondition {

    @Override
    public Query createLuceneQuery(final QueryBuilder queryBuilder) {
        Assert.notNull(queryBuilder);

        Query noTypesQuery = queryBuilder.range().onField(LuceneDescItemCondition.SPECIFICATION_ATT).from(Integer.MIN_VALUE).to(Integer.MAX_VALUE).createQuery();

        return queryBuilder.
                bool().
                must(noTypesQuery).
                not().
                createQuery();
    }
}