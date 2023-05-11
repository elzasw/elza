package cz.tacr.elza.dataexchange.output.aps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.output.writer.BaseApInfo;
import cz.tacr.elza.dataexchange.output.writer.ExternalIdApInfo;
import cz.tacr.elza.dataexchange.output.writer.ItemApInfo;
import cz.tacr.elza.dataexchange.output.writer.PartsApInfo;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApState;

public class ApInfo implements BaseApInfo, ExternalIdApInfo, PartsApInfo, ItemApInfo {

    // --- fields ---

    private final ApState apState;

    private Collection<ApBindingState> externalIds;

    private ArrayList<ApPart> parts = new ArrayList<>();

    private Map<Integer, Collection<ApItem>> partItemsMap; // partId, items

    private Map<Integer, Collection<ApIndex>> partIndexMap;

    // --- getters/setters ---

    public ApAccessPoint getAccessPoint() {
        Validate.notNull(apState);
        return apState.getAccessPoint();
    }

    @Override
    public ApState getApState() {
        return apState;
    }

    @Override
    public Collection<ApBindingState> getExternalIds() {
        return externalIds;
    }

    @Override
    public void setExternalIds(Collection<ApBindingState> externalIds) {
        this.externalIds = externalIds;
    }

    // --- constructor ---

    public ApInfo(ApState apState) {
        this.apState = apState;
    }

    @Override
    public Collection<ApPart> getParts() {
        return parts;
    }

    @Override
    public void addPart(ApPart part) {
        if (part.getPartId().equals(apState.getAccessPoint().getPreferredPartId())) {
            // store pref. name as first
            parts.add(0, part);
        } else {
            this.parts.add(part);
        }
    }


    @Override
    public Map<Integer, Collection<ApItem>> getItems() {
        return partItemsMap;
    }

    @Override
    public void setItems(final Map<Integer, Collection<ApItem>> items) {
        partItemsMap = items;
    }

    public void addItemsMap(Integer partId, Collection<ApItem> items) {

    }

    public Collection<ApItem> getItemsForPart(Integer partId) {
        Collection<ApItem> items = partItemsMap.get(partId);
        Validate.notNull(items, "Items for part not found, partId: %i", partId);
        return items;
    }

    public Map<Integer, Collection<ApIndex>> getIndexes() {
        return partIndexMap;
    }

    public void setIndexes(final Map<Integer, Collection<ApIndex>> indexMap) {
        this.partIndexMap = indexMap;

    }

    /**
     * Return list of indexes for the part
     * 
     * @param partId
     * @return
     */
    public Collection<ApIndex> getIndexesForPart(Integer partId) {
        Collection<ApIndex> items = partIndexMap.get(partId);
        return items == null ? Collections.emptyList() : items;

    }
}
