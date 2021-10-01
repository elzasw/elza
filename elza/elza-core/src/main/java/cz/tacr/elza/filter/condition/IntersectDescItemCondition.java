package cz.tacr.elza.filter.condition;

import org.apache.commons.lang3.Validate;
import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.util.Assert;

/**
 * Spadá částečně do období.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 */
public class IntersectDescItemCondition<T extends Interval<IV>, IV> extends AbstractIntervalDescItemConditionWithValue<T, IV> {

    public IntersectDescItemCondition(T conditionValue, String attributeNameFrom, String attributeNameTo) {
        super(conditionValue, attributeNameFrom, attributeNameTo);
    }

    @Override
    public Query createLuceneQuery(QueryBuilder queryBuilder) {
        Validate.notNull(queryBuilder);

        Interval<IV> interval = getValue();
        IV from = interval.getFrom();
        IV to = interval.getTo();

        // (StartA <= EndB) and (EndA >= StartB)
        // see https://stackoverflow.com/questions/325933/determine-whether-two-date-ranges-overlap
        Query fromQuery1 = queryBuilder.range().onField(getAttributeNameFrom()).below(to).createQuery();
        Query fromQuery2 = queryBuilder.range().onField(getAttributeNameTo()).above(from).createQuery();
        
        return queryBuilder.bool().must(fromQuery1).must(fromQuery2).createQuery();
    }
}
