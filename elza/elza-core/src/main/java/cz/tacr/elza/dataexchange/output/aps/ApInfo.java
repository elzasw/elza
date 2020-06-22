package cz.tacr.elza.dataexchange.output.aps;

import cz.tacr.elza.dataexchange.output.writer.BaseApInfo;
import cz.tacr.elza.dataexchange.output.writer.ExternalIdApInfo;
import cz.tacr.elza.dataexchange.output.writer.ItemApInfo;
import cz.tacr.elza.dataexchange.output.writer.PartApInfo;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApState;

import java.util.Collection;
import java.util.Map;

public class ApInfo implements BaseApInfo, ExternalIdApInfo, PartApInfo, ItemApInfo {

    // --- fields ---

    private final ApState apState;

    private Collection<ApBinding> externalIds;

    private Collection<ApPart> parts;

    private Map<Integer, Collection<ApItem>> partItemsMap; // partId, items

    private boolean partyAp;

    // --- getters/setters ---

    @Override
    public ApState getApState() {
        return apState;
    }

    @Override
    public Collection<ApBinding> getExternalIds() {
        return externalIds;
    }

    @Override
    public void setExternalIds(Collection<ApBinding> externalIds) {
        this.externalIds = externalIds;
    }

    public boolean isPartyAp() {
        return partyAp;
    }

    public void setPartyAp(boolean partyAp) {
        this.partyAp = partyAp;
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
    public void setParts(Collection<ApPart> parts) {
        this.parts = parts;
    }


    @Override
    public Map<Integer, Collection<ApItem>> getItems() {
        return partItemsMap;
    }

    @Override
    public void setItems(Map<Integer, Collection<ApItem>> items) {
        partItemsMap = items;
    }

    public void addItemsMap(Integer partId, Collection<ApItem> items) {

    }
}
