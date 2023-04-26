package cz.tacr.elza.filter.condition;

import java.util.List;

import org.apache.lucene.search.Query;
//import org.hibernate.search.query.dsl.BooleanJunction; TODO hibernate search 6
//import org.hibernate.search.query.dsl.QueryBuilder;
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
