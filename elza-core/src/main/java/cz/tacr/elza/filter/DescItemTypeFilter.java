package cz.tacr.elza.filter;

import java.util.List;

import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.filter.condition.DescItemCondition;

/**
 * Skupina filtrů pro typ atributu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 13. 4. 2016
 */
public class DescItemTypeFilter {

    /** Typ hodnoty na který se má filtr aplikovat. */
    private RulDescItemType descItemType;

    /** Třída na kterou se bude aplikovat filtr. */
    private Class<?> cls;

    /** Seznam podmínek. */
    private List<DescItemCondition> conditions;

    /**
     * Privátní konstruktor kvůli zabránění duplikování kódu v ostatních konstruktorech.
     *
     * @param descItemType typ atributu
     * @param cls třída na kterou se mají podmínky aplikovat
     * @param conditions podmínky
     */
    public DescItemTypeFilter(final RulDescItemType descItemType, final Class<?> cls, final List<DescItemCondition> conditions) {
        Assert.notNull(descItemType);
        Assert.notNull(cls);
        Assert.notEmpty(conditions);

        this.descItemType = descItemType;
        this.cls = cls;
        this.conditions = conditions;
    }

    public Query createLuceneQuery(final QueryBuilder queryBuilder) {
        Assert.notNull(queryBuilder);

        BooleanJunction<BooleanJunction> booleanJunction = queryBuilder.bool();
        booleanJunction.must(createDescItemTypeQuery(queryBuilder));

        conditions.forEach(c -> {
            booleanJunction.must(c.createLuceneQuery(queryBuilder));
        });


        return booleanJunction.createQuery();
    }

    private Query createDescItemTypeQuery(final QueryBuilder queryBuilder) {
        Integer descItemTypeId = descItemType.getDescItemTypeId();
        return queryBuilder.range().onField(ArrData.LUCENE_DESC_ITEM_TYPE_ID).from(descItemTypeId).to(descItemTypeId).
                createQuery();
    }

    public Class<?> getCls() {
        return cls;
    }
}
