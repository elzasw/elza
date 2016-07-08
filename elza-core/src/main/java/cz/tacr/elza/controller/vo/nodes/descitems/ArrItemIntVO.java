package cz.tacr.elza.controller.vo.nodes.descitems;

/**
 * VO hodnoty atributu - int.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
public class ArrItemIntVO extends ArrItemVO {

    /**
     * celé číslo
     */
    private Integer value;

    public Integer getValue() {
        return value;
    }

    public void setValue(final Integer value) {
        this.value = value;
    }
}