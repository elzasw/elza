package cz.tacr.elza.api;

import java.util.List;

public interface ArrFaLevelExt<FC extends ArrFaChange, N extends ArrNode, DI extends ArrDescItemExt> extends ArrFaLevel<FC, N> {


    List<DI> getDescItemList();

    void setDescItemList(List<DI> descItemList);

}
