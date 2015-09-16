package cz.tacr.elza.domain;

/**
 * @author Martin Šlapa
 * @since 15.9.15
 */
public class ArrDescItemString extends ArrDescItem implements cz.tacr.elza.api.ArrDescItemString<ArrChange, RulDescItemType, RulDescItemSpec, ArrNode> {

    private String value;

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
