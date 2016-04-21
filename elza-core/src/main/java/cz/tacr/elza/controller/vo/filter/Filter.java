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
    private ValuesTypes valuesTypes;

    /** Hodnoty. */
    private List<String> values;

    /** Typ vybraných specifikací. */
    private ValuesTypes specsTypes ;

    /** Specifikace. */
    private List<String> specs;

    /** Typ podmínky. */
    private Condition conditionType ;

    /** Parametry podmínky. */
    private List<String> conditions;

    public ValuesTypes getValuesTypes() {
        return valuesTypes;
    }

    public void setValuesTypes(ValuesTypes valuesTypes) {
        this.valuesTypes = valuesTypes;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public ValuesTypes getSpecsTypes() {
        return specsTypes;
    }

    public void setSpecsTypes(ValuesTypes specsTypes) {
        this.specsTypes = specsTypes;
    }

    public List<String> getSpecs() {
        return specs;
    }

    public void setSpecs(List<String> specs) {
        this.specs = specs;
    }

    public Condition getConditionType() {
        return conditionType;
    }

    public void setConditionType(Condition conditionType) {
        this.conditionType = conditionType;
    }

    public List<String> getConditions() {
        return conditions;
    }

    public void setConditions(List<String> conditions) {
        this.conditions = conditions;
    }
}
