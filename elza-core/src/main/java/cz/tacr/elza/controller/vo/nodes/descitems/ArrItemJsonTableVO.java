package cz.tacr.elza.controller.vo.nodes.descitems;

import cz.tacr.elza.domain.table.ElzaTable;

/**
 * VO hodnoty atributu - json table.
 *
 * @author Martin Šlapa
 * @since 21.06.2016
 */
public class ArrItemJsonTableVO extends ArrItemVO {

    /**
     * celé číslo
     */
    private ElzaTable value;

    public ElzaTable getValue() {
        return value;
    }

    public void setValue(final ElzaTable value) {
        this.value = value;
    }
}