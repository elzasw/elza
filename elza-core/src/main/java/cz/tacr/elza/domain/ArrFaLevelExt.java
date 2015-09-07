package cz.tacr.elza.domain;

import java.util.LinkedList;
import java.util.List;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */

public class ArrFaLevelExt extends ArrFaLevel implements cz.tacr.elza.api.ArrFaLevelExt<ArrFaChange, ArrNode, ArrDescItemExt> {

    private List<ArrDescItemExt> descItemList = new LinkedList<>();

    public List<ArrDescItemExt> getDescItemList() {
        return descItemList;
    }

    public void setDescItemList(List<ArrDescItemExt> descItemList) {
        this.descItemList = descItemList;
    }

    @Override
    public String toString() {
        return "ArrFaLevelExt pk=" + getFaLevelId();
    }
}
