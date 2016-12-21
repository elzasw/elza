package cz.tacr.elza.filter.condition;

import java.util.List;

import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.util.Assert;

/**
 * Má typ z dané množiny nebo má hodnotu bez typu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 */
public class SelectedAndEmptyPacketTypeDescItemCondition implements LuceneDescItemCondition {

    private List<Integer> values;

    public SelectedAndEmptyPacketTypeDescItemCondition(final List<Integer> values) {
        Assert.notEmpty(values);

        this.values = values;
    }

    @Override
    public Query createLuceneQuery(final QueryBuilder queryBuilder) {
        Assert.notNull(queryBuilder);

        BooleanJunction<BooleanJunction> booleanJunction = queryBuilder.bool();

        values.forEach(v -> {
            Query query = queryBuilder.range().onField(LuceneDescItemCondition.SPECIFICATION_ATT).from(v).to(v).createQuery();
            booleanJunction.should(query);
        });

        Query selectedTypesQuery = booleanJunction.createQuery();

        Query allTypesQuery = queryBuilder.range().onField(LuceneDescItemCondition.SPECIFICATION_ATT).from(Integer.MIN_VALUE).to(Integer.MAX_VALUE).createQuery();
        Query noTypesQuery = queryBuilder.bool().must(allTypesQuery).not().createQuery();

        return queryBuilder.
                bool().
                should(noTypesQuery).
                should(selectedTypesQuery).
                createQuery();
    }
}
