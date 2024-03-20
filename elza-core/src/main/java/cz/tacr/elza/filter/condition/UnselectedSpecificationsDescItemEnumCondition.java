package cz.tacr.elza.filter.condition;

import java.util.List;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

/**
 * Podmínka na výběr podle specifikací které nebyly vybrány.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 29. 4. 2016
 * @update Sergey Iryupin
 * @since 20. 3. 2024
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
	public SearchPredicate createSearchPredicate(final SearchPredicateFactory factory) {
		SearchPredicate specs = super.createSearchPredicate(factory);

		return factory.bool().mustNot(specs).toPredicate();
	}
}
