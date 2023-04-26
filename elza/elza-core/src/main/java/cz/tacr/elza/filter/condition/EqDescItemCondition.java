package cz.tacr.elza.filter.condition;

import org.apache.lucene.search.Query;
//import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.util.Assert;

/**
 * Rovno.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 */
public class EqDescItemCondition<T> extends AbstractDescItemConditionWithValue<T> {

    public EqDescItemCondition(T conditionValue, String attributeName) {
        super(conditionValue, attributeName);
    }

//    @Override
//    public Query createLuceneQuery(QueryBuilder queryBuilder) { TODO hibernate search 6
//        Assert.notNull(queryBuilder);
//
//        return queryBuilder.keyword().onField(getAttributeName()).ignoreAnalyzer().matching(getValue()).createQuery();
//    }
}
