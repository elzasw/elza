package cz.tacr.elza.filter.condition;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

//import org.apache.lucene.search.Query;
//import org.hibernate.search.query.dsl.QueryBuilder; TODO hibernate search 6

/**
 * Podmínka přes Lucene.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 2. 5. 2016
 */
public interface LuceneDescItemCondition extends DescItemCondition {

    /**
     * Vytvoří dotaz.
     *
     * @param queryBuilder queryBuilder
     *
     * @return dotaz
     */
	SearchPredicate createSearchPredicate(final SearchPredicateFactory factory); 
//    Query createLuceneQuery(QueryBuilder queryBuilder); TODO hibernate search 6
}
