package cz.tacr.elza.filter.condition;

import org.apache.commons.lang3.Validate;

/**
 * Předek pro intervalé podmínky.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 20. 4. 2016
 */
public abstract class AbstractIntervalDescItemConditionWithValue<T extends Interval<IV>, IV> implements LuceneDescItemCondition {

    private T value;
    private String attributeNameFrom;
    private String attributeNameTo;

    public AbstractIntervalDescItemConditionWithValue(final T conditionValue, final String attributeNameFrom, final String attributeNameTo) {
        Validate.notNull(conditionValue);
        Validate.notNull(attributeNameFrom);
        Validate.notNull(attributeNameTo);

        this.value = conditionValue;
        this.attributeNameFrom = attributeNameFrom;
        this.attributeNameTo = attributeNameTo;
    }

    protected T getValue() {
        return value;
    }

    protected String getAttributeNameFrom() {
        return attributeNameFrom;
    }

    protected String getAttributeNameTo() {
        return attributeNameTo;
    }
}
