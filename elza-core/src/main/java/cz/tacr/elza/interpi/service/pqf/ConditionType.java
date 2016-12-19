package cz.tacr.elza.interpi.service.pqf;

/**
 * Typy podmínek.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 29. 11. 2016
 */
public enum ConditionType {

    AND(" @and "),

    OR(" @or ");

    private String condition;

    private ConditionType(final String condition) {
        this.condition = condition;
    }

    public String getCondition() {
        return condition;
    }
}
