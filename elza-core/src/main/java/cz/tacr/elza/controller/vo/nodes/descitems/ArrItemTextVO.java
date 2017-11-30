package cz.tacr.elza.controller.vo.nodes.descitems;

/**
 * VO hodnoty atributu - text.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
public class ArrItemTextVO extends ArrItemVO {

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
