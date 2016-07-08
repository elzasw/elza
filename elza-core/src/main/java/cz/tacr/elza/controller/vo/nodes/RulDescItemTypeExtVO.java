package cz.tacr.elza.controller.vo.nodes;

import java.util.List;

import cz.tacr.elza.controller.vo.RulDescItemTypeVO;


/**
 * VO rozšířený typu hodnoty atributu
 *
 * @author Martin Šlapa
 * @since 14.1.2016
 */
public class RulDescItemTypeExtVO extends RulDescItemTypeVO {

    /**
     * seznam rozšířených specifikací atributu
     */
    private List<RulDescItemSpecExtVO> descItemSpecs;

    public List<RulDescItemSpecExtVO> getDescItemSpecs() {
        return descItemSpecs;
    }

    public void setDescItemSpecs(final List<RulDescItemSpecExtVO> descItemSpecs) {
        this.descItemSpecs = descItemSpecs;
    }

}
