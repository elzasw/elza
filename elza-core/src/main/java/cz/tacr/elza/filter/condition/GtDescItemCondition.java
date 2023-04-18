package cz.tacr.elza.filter.condition;

import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.util.Assert;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 */
public class GtDescItemCondition<T> extends AbstractDescItemConditionWithValue<T> {

    public GtDescItemCondition(final T conditionValue, final String attributeName) {
        super(conditionValue, attributeName);
    }

    @Override
    public Query createLuceneQuery(final QueryBuilder queryBuilder) {
        Assert.notNull(queryBuilder);

        return queryBuilder.range().onField(getAttributeName()).above(getValue()).excludeLimit().createQuery();
    }

}
