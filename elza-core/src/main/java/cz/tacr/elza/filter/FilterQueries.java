package cz.tacr.elza.filter;

import java.util.LinkedList;
import java.util.List;

import org.hibernate.search.engine.search.predicate.SearchPredicate;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 20. 10. 2016
 */
public class FilterQueries {

    private List<SearchPredicate> lucenePredicates = new LinkedList<>();
    private List<jakarta.persistence.Query> hibernateQueries = new LinkedList<>();

    /**
     * @param lucenePredicates
     * @param hibernateQueries
     */
    public FilterQueries(final List<SearchPredicate> lucenePredicates, final List<jakarta.persistence.Query> hibernateQueries) {
        this.lucenePredicates = lucenePredicates;
        this.hibernateQueries = hibernateQueries;
    }

    public List<SearchPredicate> getLucenePredicates() {
        return lucenePredicates;
    }

    public List<jakarta.persistence.Query> getHibernateQueries() {
        return hibernateQueries;
    }
}
