package cz.tacr.elza.domain;

import java.util.LinkedList;
import java.util.List;


/**
 *  Rozšíření {@link ArrLevel} o atributy archivního popisu včetně hodnot.
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */

public class ArrLevelExt extends ArrLevel implements cz.tacr.elza.api.ArrLevelExt<ArrChange, ArrNode, ArrDescItem> {

    private ArrNodeConformityExt nodeConformityInfo;

    private List<ArrDescItem> descItemList = new LinkedList<>();

    public List<ArrDescItem> getDescItemList() {
        return descItemList;
    }

    public void setDescItemList(List<ArrDescItem> descItemList) {
        this.descItemList = descItemList;
    }


    public ArrNodeConformityExt getNodeConformityInfo() {
        return nodeConformityInfo;
    }

    public void setNodeConformityInfo(final ArrNodeConformityExt nodeConformityInfo) {
        this.nodeConformityInfo = nodeConformityInfo;
    }

    @Override
    public String toString() {
        return "ArrLevelExt pk=" + getLevelId();
    }
}
