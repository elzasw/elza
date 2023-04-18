package cz.tacr.elza.filter.condition;

import java.util.List;

import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.QueryBuilder;


/**
 * Podmínka na výběr podle hodnot které nebyly vybrány.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 13. 4. 2016
 */
public class UnselectedValuesDescItemEnumCondition extends SelectedValuesDescItemEnumCondition {

    /**
     * Konstruktor.
     *
     * @param values vybrané hodnoty
     * @param attributeName název atributu pro který je podmínka určena
     */
   public UnselectedValuesDescItemEnumCondition(final List<String> values,final String attributeName) {
       super(values, attributeName);
   }

   @Override
   public Query createLuceneQuery(final QueryBuilder queryBuilder) {
       Query query = super.createLuceneQuery(queryBuilder);

       return queryBuilder.bool().must(query).not().createQuery();
   }
}
