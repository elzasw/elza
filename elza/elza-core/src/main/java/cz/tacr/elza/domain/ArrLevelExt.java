package cz.tacr.elza.domain;

import java.util.LinkedList;
import java.util.List;


/**
 * Rozšíření {@link ArrLevel} o atributy archivního popisu včetně hodnot.
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */

public class ArrLevelExt extends ArrLevel {

    private ArrNodeConformityExt nodeConformityInfo;

    private List<ArrDescItem> descItemList = new LinkedList<>();

    /**
     *
     * @return atributy archivního popisu včetně hodnot.
     */
    public List<ArrDescItem> getDescItemList() {
        return descItemList;
    }

    public void setDescItemList(final List<ArrDescItem> descItemList) {
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
