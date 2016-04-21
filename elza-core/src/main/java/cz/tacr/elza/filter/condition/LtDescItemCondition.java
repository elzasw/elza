package cz.tacr.elza.filter.condition;

import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.util.Assert;

/**
 * Menší než.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 */
public class LtDescItemCondition<T> extends AbstractDescItemConditionWithValue<T> {

    public LtDescItemCondition(T conditionValue, String attributeName) {
        super(conditionValue, attributeName);
    }

    @Override
    public Query createLuceneQuery(QueryBuilder queryBuilder) {
        Assert.notNull(queryBuilder);

        return queryBuilder.range().onField(getAttributeName()).below(getValue()).excludeLimit().createQuery();
    }
}
