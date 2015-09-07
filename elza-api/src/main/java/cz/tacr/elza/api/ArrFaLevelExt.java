package cz.tacr.elza.api;

import java.util.List;

/**
 * Rozšíření {@link ArrFaLevel} o atributy archivního popisu včetně hodnot.
 * @author vavrejn
 *
 * @param <FC>
 * @param <N>
 * @param <DI>
 */
public interface ArrFaLevelExt<FC extends ArrFaChange, N extends ArrNode, DI extends ArrDescItemExt> extends ArrFaLevel<FC, N> {

    /**
     * 
     * @return atributy archivního popisu včetně hodnot.
     */
    List<DI> getDescItemList();

    void setDescItemList(List<DI> descItemList);

}
