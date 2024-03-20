package cz.tacr.elza.filter.condition;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

/**
 * Podmínka přes Lucene.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 2. 5. 2016
 * @update Sergey Iryupin
 * @since 20. 3. 2024
 */
public interface LuceneDescItemCondition extends DescItemCondition {

    /**
     * Vytvoří podmínku vyhledávání.
     *
     * @param SearchPredicateFactory factory
     *
     * @return predicate
     */
	SearchPredicate createSearchPredicate(final SearchPredicateFactory factory); 
}
