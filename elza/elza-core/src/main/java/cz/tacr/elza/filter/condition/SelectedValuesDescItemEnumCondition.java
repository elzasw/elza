package cz.tacr.elza.filter.condition;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.Validate;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

/**
 * Podmínka na výběr podle hodnot které byly vybrány.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 * @update Sergey Iryupin
 * @since 20. 3. 2024
 */
public class SelectedValuesDescItemEnumCondition implements LuceneDescItemCondition {

    /** Hodnoty. */
    private List<String> values;
    private String attributeName;

    /**
     * Konstruktor.
     *
     * @param values vybrané hodnoty
     */
    public SelectedValuesDescItemEnumCondition(final List<String> values, final String attributeName) {
        Validate.notEmpty(values);
        Objects.requireNonNull(attributeName);

        this.values = values;
        this.attributeName = attributeName;
    }

	@Override
	public SearchPredicate createSearchPredicate(final SearchPredicateFactory factory) {
		BooleanPredicateClausesStep<?> bool = factory.bool();

		values.forEach(v -> {
			bool.should(factory.wildcard().field(attributeName).matching(v.toLowerCase()).toPredicate());
		});

		return bool.toPredicate();
	}
}
