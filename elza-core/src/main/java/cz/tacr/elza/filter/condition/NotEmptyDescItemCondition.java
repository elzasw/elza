package cz.tacr.elza.filter.condition;

import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.util.Assert;

/**
 * Má vyplněnu nějakou hodnotu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 */
public class NotEmptyDescItemCondition implements DescItemCondition {

    @Override
    public Query createLuceneQuery(QueryBuilder queryBuilder) {
        Assert.notNull(queryBuilder);

        return queryBuilder.keyword().wildcard().onField(DescItemCondition.FULLTEXT_ATT).matching("?*").createQuery();
    }
}
