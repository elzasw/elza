package cz.tacr.elza.filter.condition;

import java.util.List;

import org.apache.commons.lang3.Validate;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

/**
 * Podmínka na výběr podle hodnot které byly vybrány.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
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
        Validate.notNull(attributeName);

        this.values = values;
        this.attributeName = attributeName;
    }

	@Override
	public SearchPredicate createSearchPredicate(final SearchPredicateFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

//    @Override TODO hibernate search 6
//    public Query createLuceneQuery(final QueryBuilder queryBuilder) {
//        BooleanJunction<BooleanJunction> booleanJunction = queryBuilder.bool();
//
//        values.forEach(v -> {
//            Query query = queryBuilder.keyword().wildcard().onField(attributeName).matching(v.toLowerCase()).createQuery();
//            booleanJunction.should(query);
//        });
//
//        return booleanJunction.createQuery();
//    }
}
