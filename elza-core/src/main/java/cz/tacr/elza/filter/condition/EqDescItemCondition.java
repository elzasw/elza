package cz.tacr.elza.filter.condition;

import org.apache.commons.lang3.Validate;
import org.apache.lucene.search.Query;
//import org.hibernate.search.query.dsl.QueryBuilder;

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
//    public Query createLuceneQuery(QueryBuilder queryBuilder) {  TODO hibernate search 6
//        Validate.notNull(queryBuilder);
//
//        return queryBuilder.keyword().onField(getAttributeName()).ignoreAnalyzer().matching(getValue()).createQuery();
//    }
}
