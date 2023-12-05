package cz.tacr.elza.filter.condition;

import org.apache.lucene.search.Query;
//import org.hibernate.search.query.dsl.QueryBuilder; TODO hibernate search 6
import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrDescItem;

/**
 * Má vyplněnu nějakou hodnotu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 */
public class NotEmptyDescItemCondition implements LuceneDescItemCondition {

//    @Override TODO hibernate search 6
//    public Query createLuceneQuery(final QueryBuilder queryBuilder) {
//        Assert.notNull(queryBuilder);
//
//		return queryBuilder.keyword().wildcard().onField(ArrDescItem.FULLTEXT_ATT).matching("?*").createQuery();
//    }
}
