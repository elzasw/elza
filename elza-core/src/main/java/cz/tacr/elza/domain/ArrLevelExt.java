package cz.tacr.elza.domain;

import java.util.LinkedList;
import java.util.List;


/**
 *  Rozšíření {@link ArrLevel} o atributy archivního popisu včetně hodnot.
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */

public class ArrLevelExt extends ArrLevel implements cz.tacr.elza.api.ArrLevelExt<ArrChange, ArrNode, ArrDescItemExt> {

    private List<ArrDescItemExt> descItemList = new LinkedList<>();

    public List<ArrDescItemExt> getDescItemList() {
        return descItemList;
    }

    public void setDescItemList(List<ArrDescItemExt> descItemList) {
        this.descItemList = descItemList;
    }

    @Override
    public String toString() {
        return "ArrLevelExt pk=" + getLevelId();
    }
}
