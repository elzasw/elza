package cz.tacr.elza.filter.condition;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.Validate;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

/**
 * Podmínka na výběr podle specifikací které byly vybrány.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 29. 4. 2016
 */
public class SelectedSpecificationsDescItemEnumCondition implements LuceneDescItemCondition {

    /** Hodnoty. */
    private List<Integer> values;
    private String attributeName;

    /**
     * Konstruktor.
     *
     * @param values vybrané hodnoty
     */
    public SelectedSpecificationsDescItemEnumCondition(final List<Integer> values, final String attributeName) {
        Validate.notEmpty(values);
        Objects.requireNonNull(attributeName);

        this.values = values;
        this.attributeName = attributeName;
    }

	@Override
	public SearchPredicate createSearchPredicate(final SearchPredicateFactory factory) {
		BooleanPredicateClausesStep<?> bool = factory.bool();

		values.forEach(v -> {
			bool.should(factory.match().field(attributeName).matching(v.toString()).toPredicate());
		});

		return bool.toPredicate();
	}

//    @Override TODO hibernate search 6
//    public Query createLuceneQuery(final QueryBuilder queryBuilder) {
//        BooleanJunction<BooleanJunction> booleanJunction = queryBuilder.bool();
//
//        values.forEach(v -> {
//            Query query = queryBuilder.range().onField(attributeName).from(v).to(v).createQuery();
//            booleanJunction.should(query);
//        });
//
//        return booleanJunction.createQuery();
//    }
}
