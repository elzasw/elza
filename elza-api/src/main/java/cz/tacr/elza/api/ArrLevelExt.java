package cz.tacr.elza.api;

import java.util.List;

/**
 * Rozšíření {@link ArrLevel} o atributy archivního popisu včetně hodnot.
 * @author vavrejn
 *
 * @param <FC> {@link ArrChange}
 * @param <N> {@link ArrNode}
 * @param <DI> {@link ArrDescItemExt}
 */
public interface ArrLevelExt<FC extends ArrChange, N extends ArrNode, DI extends ArrDescItemExt> extends ArrLevel<FC, N> {

    /**
     * 
     * @return atributy archivního popisu včetně hodnot.
     */
    List<DI> getDescItemList();

    void setDescItemList(List<DI> descItemList);

}
