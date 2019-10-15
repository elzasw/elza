package cz.tacr.elza.interpi.service.vo;

import cz.tacr.elza.interpi.service.pqf.AttributeType;
import cz.tacr.elza.interpi.service.pqf.ConditionType;

/**
 * Vyhledávací podmínka.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 29. 11. 2016
 */
public class ConditionVO {

    private ConditionType conditionType;
    private AttributeType attType;
    private String value;

    public ConditionVO() {

    }

    public ConditionVO(final ConditionType conditionType, final AttributeType attType, final String value) {
        this.conditionType = conditionType;
        this.attType = attType;
        this.value = value;
    }

    public ConditionType getConditionType() {
        return conditionType;
    }
    public AttributeType getAttType() {
        return attType;
    }
    public String getValue() {
        return value;
    }
}
