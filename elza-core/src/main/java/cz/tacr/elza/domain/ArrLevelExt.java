package cz.tacr.elza.domain;

import java.util.LinkedList;
import java.util.List;


/**
 *  Rozšíření {@link ArrLevel} o atributy archivního popisu včetně hodnot.
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */

public class ArrLevelExt extends ArrLevel implements cz.tacr.elza.api.ArrLevelExt<ArrChange, ArrNode, ArrDescItem> {

    private List<ArrDescItem> descItemList = new LinkedList<>();

    public List<ArrDescItem> getDescItemList() {
        return descItemList;
    }

    public void setDescItemList(List<ArrDescItem> descItemList) {
        this.descItemList = descItemList;
    }

    @Override
    public String toString() {
        return "ArrLevelExt pk=" + getLevelId();
    }
}
