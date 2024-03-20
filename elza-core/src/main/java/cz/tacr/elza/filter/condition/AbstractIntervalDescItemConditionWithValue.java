package cz.tacr.elza.filter.condition;

import java.util.Objects;

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
    	Objects.requireNonNull(conditionValue);
    	Objects.requireNonNull(attributeNameFrom);
    	Objects.requireNonNull(attributeNameTo);

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
