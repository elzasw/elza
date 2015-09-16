package cz.tacr.elza.domain;

/**
 * @author Martin Šlapa
 * @since 15.9.15
 */
public class ArrDescItemInt extends ArrDescItem implements cz.tacr.elza.api.ArrDescItemInt<ArrChange, RulDescItemType, RulDescItemSpec, ArrNode> {

    private Integer value;

    @Override
    public Integer getValue() {
        return value;
    }

    @Override
    public void setValue(Integer value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
