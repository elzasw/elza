package cz.tacr.elza.filter.condition;

import java.util.List;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.springframework.util.Assert;

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
        Assert.notEmpty(values);
        Assert.notNull(attributeName);

        this.values = values;
        this.attributeName = attributeName;
    }

    @Override
    public SearchPredicate createLucenePredicate(SearchPredicateFactory factory) {
        BooleanPredicateClausesStep<?> boolStep = factory.bool();

        values.forEach(v -> {
            SearchPredicate predicate = factory.range().field(attributeName).between(v, v).toPredicate();
            boolStep.should(predicate);
        });

        return boolStep.toPredicate();
    }
}
