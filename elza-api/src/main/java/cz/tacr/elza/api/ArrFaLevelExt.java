package cz.tacr.elza.api;

import java.util.List;

public interface ArrFaLevelExt extends ArrFaLevel {


    List<ArrDescItemExt> getDescItemList();

    void setDescItemList(List<ArrDescItemExt> descItemList);
}
