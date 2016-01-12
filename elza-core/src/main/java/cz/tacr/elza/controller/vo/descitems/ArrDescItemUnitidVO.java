package cz.tacr.elza.controller.vo.descitems;

/**
 * VO hodnoty atributu - unit id.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
public class ArrDescItemUnitidVO extends ArrDescItemVO {

    /**
     * unikátní identifikátor
     */
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }
}