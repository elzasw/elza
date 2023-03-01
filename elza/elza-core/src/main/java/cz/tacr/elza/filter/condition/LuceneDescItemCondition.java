package cz.tacr.elza.filter.condition;

import org.apache.lucene.search.Query;
import org.apache.lucene.util.QueryBuilder;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

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
     * @param factory factory
     *
     * @return dotaz
     */
    SearchPredicate createLucenePredicate(SearchPredicateFactory factory);
}
