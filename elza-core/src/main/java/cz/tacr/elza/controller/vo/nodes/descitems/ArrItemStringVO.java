package cz.tacr.elza.controller.vo.nodes.descitems;

/**
 * VO hodnoty atributu - string.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
public class ArrItemStringVO extends ArrItemVO {

    /**
     * textový řetězec
     */
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }
}