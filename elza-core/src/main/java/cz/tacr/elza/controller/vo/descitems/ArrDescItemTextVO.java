package cz.tacr.elza.controller.vo.descitems;

/**
 * VO hodnoty atributu - text.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
public class ArrDescItemTextVO extends ArrDescItemVO {

    /**
     * text
     */
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }
}