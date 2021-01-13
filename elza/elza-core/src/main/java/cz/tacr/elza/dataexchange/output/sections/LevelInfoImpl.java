package cz.tacr.elza.dataexchange.output.sections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cz.tacr.elza.dataexchange.output.writer.LevelInfo;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrItem;

public class LevelInfoImpl implements LevelInfo {

    private final List<ArrItem> items = new ArrayList<>();

    private final int nodeId;

    private final Integer parentNodeId;

    private String nodeUuid;

    /**
     * Collection of DAOs
     * 
     * Most levels are without DAOs
     */
    private List<ArrDao> daos;

    public LevelInfoImpl(int nodeId, Integer parentNodeId) {
        this.nodeId = nodeId;
        this.parentNodeId = parentNodeId;
    }

    @Override
    public int getNodeId() {
        return nodeId;
    }

    @Override
    public Integer getParentNodeId() {
        return parentNodeId;
    }

    @Override
    public String getNodeUuid() {
        return nodeUuid;
    }

    public void setNodeUuid(String nodeUuid) {
        this.nodeUuid = nodeUuid;
    }

    @Override
    public List<ArrItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItem(ArrItem item) {
        items.add(item);
    }

    @Override
    public Collection<ArrDao> getDaos() {
        return daos != null ? daos : Collections.emptyList();
    }

    public void addDao(ArrDao dao) {
        if (daos == null) {
            daos = new ArrayList<>();
        }
        daos.add(dao);
    }

}
