package cz.tacr.elza.dataexchange.output.sections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cz.tacr.elza.dataexchange.output.writer.DaoInfo;
import cz.tacr.elza.dataexchange.output.writer.LevelInfo;
import cz.tacr.elza.domain.ArrInhibitedItem;
import cz.tacr.elza.domain.ArrItem;

public class LevelInfoImpl implements LevelInfo {

    private final List<ArrItem> items = new ArrayList<>();

    private final List<ArrInhibitedItem> inhibitedItems = new ArrayList<>(); 

    private final int nodeId;

    private final Integer parentNodeId;

    private String nodeUuid;

    /**
     * Collection of DAOs
     *
     * Most levels are without DAOs
     */
    private List<DaoInfo> daos;

    public LevelInfoImpl(LevelInfoImpl source) {
        items.addAll(source.getItems());
        inhibitedItems.addAll(source.getInhibitedItems());
        nodeId = source.getNodeId();
        parentNodeId = source.getParentNodeId();
        nodeUuid = source.getNodeUuid();
        if (!source.getDaos().isEmpty()) {
            daos = new ArrayList<>();
            daos.addAll(source.getDaos());
        }
    }

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

    public void addItems(Collection<ArrItem> items) {
        this.items.addAll(items);
    }
    
    public void removeItems(Collection<ArrItem> items) {
        this.items.removeAll(items);
    }

    @Override
    public List<ArrInhibitedItem> getInhibitedItems() {
		return Collections.unmodifiableList(inhibitedItems);
	}

    public void addInhibitedItems(Collection<ArrInhibitedItem> items) {
        this.inhibitedItems.addAll(items);
    }

	@Override
    public Collection<DaoInfo> getDaos() {
        return daos != null ? daos : Collections.emptyList();
    }

    public void addDao(DaoInfo dao) {
        if (daos == null) {
            daos = new ArrayList<>();
        }
        daos.add(dao);
    }

    public void removeDao() {
        daos = null;
    }
}
