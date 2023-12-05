package cz.tacr.elza.filter.condition;

import org.apache.commons.lang3.Validate;

/**
 * Abstraktní předek pro podmínky oproti nějaké hodnotě.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 */
public abstract class AbstractDescItemConditionWithValue<T> implements LuceneDescItemCondition {

    private T value;
    private String attributeName;

    /**
     * Konstruktor.
     *
     * @param conditionValue hodnota podmínky
     * @param attributeName název atributu pro který je podmínka určena
     */
    public AbstractDescItemConditionWithValue(final T conditionValue, final String attributeName) {
        Validate.notNull(conditionValue);
        Validate.notNull(attributeName);

        this.value = conditionValue;
        this.attributeName = attributeName;
    }

    protected T getValue() {
        return value;
    }

    protected String getAttributeName() {
        return attributeName;
    }
}
