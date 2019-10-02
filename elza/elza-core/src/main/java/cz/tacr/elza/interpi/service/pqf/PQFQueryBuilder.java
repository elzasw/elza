package cz.tacr.elza.interpi.service.pqf;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import cz.tacr.elza.interpi.service.vo.ConditionVO;

/**
 * Podpora pro tvorbu PQF dotazu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 6. 12. 2016
 */
public class PQFQueryBuilder {

    /** Typ vyhledávaných dat sosby/rejstříky/oboje. */
    private Boolean isParty;

    /** Příznak zda se má aplikovat pravostranný like. */
    private boolean extend = false;

    /** Seznam podmínek. */
    private List<ConditionVO> conditions;

    /**
     * @param conditions musí být předána alespoň jedna podmínka
     */
    public PQFQueryBuilder(final List<ConditionVO> conditions) {
        Assert.notNull(conditions);
        Assert.notEmpty(conditions);

        this.conditions = conditions;
    }

    public String createQuery() {
        String nameQuery = createNameQuery(conditions);
        if (StringUtils.isBlank(nameQuery)) {
            return null;
        }

        String typeQuery = null;
        if (isParty != null) {
            typeQuery = createTypeQuery(isParty);
        }

        String extendQuery = null;
        if (extend) {
            extendQuery = AttributeType.EXTEND.getAtt();
        }

        return combineQueries(extendQuery, typeQuery, nameQuery);
    }

    private String createNameQuery(final List<ConditionVO> conditions) {
        Set<String> preferredNames = new HashSet<>();
        Set<String> allNames = new HashSet<>();

        for (ConditionVO condition : conditions) {
            if (ConditionType.AND == condition.getConditionType() || condition.getConditionType() == null) { // po dohodě s Honzou Vejskalem se použije jen AND
                String value = condition.getValue();
                if (AttributeType.ALL_NAMES == condition.getAttType() && StringUtils.isNotBlank(value)) {
                    addValue(allNames, value);
                } else if (AttributeType.PREFFERED_NAME == condition.getAttType() && StringUtils.isNotBlank(value)) {
                    addValue(preferredNames, value);
                }
            }
        }

        String preferredNamesQuery = null;
        if (!preferredNames.isEmpty()) {
            preferredNamesQuery = createQuery(preferredNames, ConditionType.AND, AttributeType.PREFFERED_NAME);
        }

        String allNamesQuery = null;
        if (!allNames.isEmpty()) {
            allNamesQuery = createQuery(allNames, ConditionType.AND, AttributeType.ALL_NAMES);
        }

        String nameQuery;
        if (StringUtils.isNotBlank(preferredNamesQuery) && StringUtils.isNotBlank(allNamesQuery)) {
            StringBuilder sb = new StringBuilder(ConditionType.AND.getCondition()).
                    append(preferredNamesQuery).
                    append(" ").
                    append(allNamesQuery);
            nameQuery = sb.toString();
        } else if (StringUtils.isNotBlank(preferredNamesQuery)) {
            nameQuery = preferredNamesQuery;
        } else {
            nameQuery = allNamesQuery;
        }

        return nameQuery;
    }

    private void addValue(final Set<String> names, final String value) {
        names.add("'" + value + "'");
    }

    private String combineQueries(final String extendQuery, final String typeQuery, final String nameQuery) {
        List<String> queries = new LinkedList<>();
        if (StringUtils.isNotBlank(extendQuery)) {
            queries.add(extendQuery);
        }

        if (StringUtils.isNotBlank(typeQuery)) {
            queries.add(typeQuery);
        }

        queries.add(nameQuery);

        String query = null;
        switch (queries.size()) {
            case 1:
                query = queries.get(0);
                break;
            case 2:
                query = new StringBuilder(ConditionType.AND.getCondition()).
                    append(queries.get(0)).
                    append(queries.get(1)).
                    toString();
                break;
            case 3:
                query = new StringBuilder(queries.get(0)).
                    append(ConditionType.AND.getCondition()).
                    append(queries.get(1)).
                    append(queries.get(2)).
                    toString();
                break;
        }

        return query;
    }

    private String createTypeQuery(final boolean isParty) {
        List<String> types = getTypes(isParty);

        return createQuery(types, ConditionType.OR, AttributeType.TYPE);
    }

    private List<String> getTypes(final boolean isParty) {
        List<String> types = new LinkedList<>();
        for (InterpiRegistryType interpiRegistryType : InterpiRegistryType.values()) {
            if (interpiRegistryType.isParty() == isParty) {
                types.add(interpiRegistryType.getValue());
            }
        }

        return types;
    }

    private String createQuery(final Collection<String> types, final ConditionType conditionType, final AttributeType attributeType) {
        StringBuilder sb = new StringBuilder(attributeType.getAtt());

        for (int i = 0; i < types.size() - 1; i++) {
            sb.append(conditionType.getCondition());
        }

        for (String type : types) {
            sb.append(type);
            sb.append(" ");
        }

        return sb.toString();
    }

    /**
     * Nastaví zda se mají vyhledávat osoby, rejstříky nebo oboje.
     * Pokud se metoda nezavolá budou se hledat jak osoby tak rejstříky.
     *
     * @param isParty pokud je true hledají se jen osoby
     * 				  pojud je false heldají se jen rejstříky
     */
    public PQFQueryBuilder party(final boolean isParty) {
        this.isParty = isParty;

        return this;
    }

    /**
     * Nastaví zda se má na podmínky uplatnit pravostranný like.
     * Výchozí stav je že se like nepoužije.
     */
    public PQFQueryBuilder extend() {
        this.extend = true;

        return this;
    }
}
