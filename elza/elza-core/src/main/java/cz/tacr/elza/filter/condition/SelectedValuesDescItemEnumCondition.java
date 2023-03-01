package cz.tacr.elza.filter.condition;

import java.util.List;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.springframework.util.Assert;


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
        Assert.notEmpty(values);
        Assert.notNull(attributeName);

        this.values = values;
        this.attributeName = attributeName;
    }

    @Override
    public SearchPredicate createLucenePredicate(final SearchPredicateFactory factory) {

        BooleanPredicateClausesStep<?> boolStep = factory.bool();

        values.forEach(v -> {
            SearchPredicate predicate = factory.wildcard().field(attributeName).matching(v.toLowerCase()).toPredicate();
            boolStep.should(predicate);
        });

        return boolStep.toPredicate();
    }
}
