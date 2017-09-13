package cz.tacr.elza.filter.condition;

import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.QueryBuilder;

/**
 * Podmínka přes Lucene.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 2. 5. 2016
 */
public interface LuceneDescItemCondition extends DescItemCondition {

    String INTGER_ATT = "valueInt";

    String DECIMAL_ATT = "valueDecimal";

    String FULLTEXT_ATT = "fulltextValue";

    String NORMALIZED_FROM_ATT = "normalizedFrom";

    String NORMALIZED_TO_ATT = "normalizedTo";

    String SPECIFICATION_ATT = "specification";

    /**
     * Vytvoří dotaz.
     *
     * @param queryBuilder queryBuilder
     *
     * @return dotaz
     */
    Query createLuceneQuery(QueryBuilder queryBuilder);
}
