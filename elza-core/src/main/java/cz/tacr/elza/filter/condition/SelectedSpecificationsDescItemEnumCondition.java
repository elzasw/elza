package cz.tacr.elza.filter.condition;

import java.util.List;

import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.util.Assert;

/**
 * Podmínka na výběr podle specifikací které byly vybrány.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 29. 4. 2016
 */
public class SelectedSpecificationsDescItemEnumCondition implements DescItemCondition {

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
    public Query createLuceneQuery(final QueryBuilder queryBuilder) {
        BooleanJunction<BooleanJunction> booleanJunction = queryBuilder.bool();

        values.forEach(v -> {
            Query query = queryBuilder.range().onField(attributeName).from(v).to(v).createQuery();
            booleanJunction.should(query);
        });

        return booleanJunction.createQuery();
    }
}
