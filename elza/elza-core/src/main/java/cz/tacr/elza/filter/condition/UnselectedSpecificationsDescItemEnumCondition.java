package cz.tacr.elza.filter.condition;

import java.util.List;

import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.QueryBuilder;

/**
 * Podmínka na výběr podle specifikací které nebyly vybrány.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 29. 4. 2016
 */
public class UnselectedSpecificationsDescItemEnumCondition extends SelectedSpecificationsDescItemEnumCondition {

    /**
     * Konstruktor.
     *
     * @param values vybrané hodnoty
     * @param attributeName název atributu pro který je podmínka určena
     */
   public UnselectedSpecificationsDescItemEnumCondition(final List<Integer> values,final String attributeName) {
       super(values, attributeName);
   }

   @Override
   public Query createLuceneQuery(final QueryBuilder queryBuilder) {
       Query query = super.createLuceneQuery(queryBuilder);

       return queryBuilder.bool().must(query).not().createQuery();
   }
}
