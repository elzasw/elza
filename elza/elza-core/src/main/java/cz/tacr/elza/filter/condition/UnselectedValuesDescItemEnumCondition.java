package cz.tacr.elza.filter.condition;

import java.util.List;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

/**
 * Podmínka na výběr podle hodnot které nebyly vybrány.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 13. 4. 2016
 * @update Sergey Iryupin
 * @since 20. 3. 2024
 */
public class UnselectedValuesDescItemEnumCondition extends SelectedValuesDescItemEnumCondition {

    /**
     * Konstruktor.
     *
     * @param values vybrané hodnoty
     * @param attributeName název atributu pro který je podmínka určena
     */
   public UnselectedValuesDescItemEnumCondition(final List<String> values, final String attributeName) {
       super(values, attributeName);
   }

	@Override
	public SearchPredicate createSearchPredicate(final SearchPredicateFactory factory) {
		SearchPredicate predicate = super.createSearchPredicate(factory);

		return factory.bool().mustNot(predicate).toPredicate();
	}
}
