package cz.tacr.elza.api;

import java.util.List;

public interface ArrFaLevelExt<FC extends ArrFaChange, DI extends ArrDescItemExt> extends ArrFaLevel<FC> {


    List<DI> getDescItemList();

    void setDescItemList(List<DI> descItemList);
}
