package cz.tacr.elza.controller.vo.descitems;

/**
 * VO hodnoty atributu - coordinates.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
public class ArrDescItemCoordinatesVO extends ArrDescItemVO {

    /**
     * souřadnice
     */
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }
}