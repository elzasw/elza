package cz.tacr.elza.dataexchange.output.aps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import cz.tacr.elza.dataexchange.output.loaders.BaseLoadDispatcher;
import cz.tacr.elza.domain.ApIndex;

public abstract class IndexDispatcher extends BaseLoadDispatcher<ApIndex> {

    private final Map<Integer, Collection<ApIndex>> partIndexMap = new HashMap<>();

    private Collection<ApIndex> indexList = new ArrayList<>();

    public IndexDispatcher() {
    }

    @Override
    public void onLoad(ApIndex result) {
        indexList.add(result);

        Collection<ApIndex> partIndexList = partIndexMap.computeIfAbsent(result.getPartId(),
                                                                         (partId) -> new ArrayList<>());
        partIndexList.add(result);
    }

    public Map<Integer, Collection<ApIndex>> getPartIndexMap() {
        return partIndexMap;
    }

    public Collection<ApIndex> getIndexList() {
        return indexList;
    }

    /* @Override
    protected void onCompleted() {
    
    }
    */
}
