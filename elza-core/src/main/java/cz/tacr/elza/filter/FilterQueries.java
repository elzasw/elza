package cz.tacr.elza.filter;

import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.search.Query;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 20. 10. 2016
 */
public class FilterQueries {

    private List<Query> luceneQueries = new LinkedList<>();
    private List<jakarta.persistence.Query> hibernateQueries = new LinkedList<>();

    /**
     * @param luceneQueries
     * @param hibernateQueries
     */
    public FilterQueries(final List<Query> luceneQueries, final List<jakarta.persistence.Query> hibernateQueries) {
        this.luceneQueries = luceneQueries;
        this.hibernateQueries = hibernateQueries;
    }

    public List<Query> getLuceneQueries() {
        return luceneQueries;
    }

    public List<jakarta.persistence.Query> getHibernateQueries() {
        return hibernateQueries;
    }
}
