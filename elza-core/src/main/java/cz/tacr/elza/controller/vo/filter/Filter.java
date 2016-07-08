package cz.tacr.elza.controller.vo.filter;

import java.util.List;

/**
 * Filtr.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 18. 4. 2016
 */
public class Filter {

    /** Typ vybraných hodnot. */
    private ValuesTypes valuesType;

    /** Hodnoty. */
    private List<String> values;

    /** Typ vybraných specifikací. */
    private ValuesTypes specsType;

    /** Specifikace. */
    private List<Integer> specs;

    /** Typ podmínky. */
    private Condition conditionType ;

    /** Parametry podmínky. */
    private List<String> condition;

    public ValuesTypes getValuesType() {
        return valuesType;
    }

    public void setValuesType(final ValuesTypes valuesType) {
        this.valuesType = valuesType;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(final List<String> values) {
        this.values = values;
    }

    public ValuesTypes getSpecsType() {
        return specsType;
    }

    public void setSpecsType(final ValuesTypes specsType) {
        this.specsType = specsType;
    }

    public List<Integer> getSpecs() {
        return specs;
    }

    public void setSpecs(final List<Integer> specs) {
        this.specs = specs;
    }

    public Condition getConditionType() {
        return conditionType;
    }

    public void setConditionType(final Condition conditionType) {
        this.conditionType = conditionType;
    }

    public List<String> getCondition() {
        return condition;
    }

    public void setCondition(final List<String> condition) {
        this.condition = condition;
    }
}
