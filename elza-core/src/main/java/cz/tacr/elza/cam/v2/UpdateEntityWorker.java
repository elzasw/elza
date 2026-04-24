package cz.tacr.elza.cam.v2;

import java.util.Map;

import cz.tacr.cam.v2.schema.cam.BatchUpdateXml;

public class UpdateEntityWorker implements UploadWorker {

    final private BatchUpdateXml updateXml;
    final private Map<Integer, String> itemUuidMap;
    final private Map<Integer, String> partUuidMap;

    public UpdateEntityWorker(final BatchUpdateXml updateXml,
                              final Map<Integer, String> itemUuidMap,
                              final Map<Integer, String> partUuidMap) {
        this.updateXml = updateXml;
        this.itemUuidMap = itemUuidMap;
        this.partUuidMap = partUuidMap;
    }

    @Override
    public BatchUpdateXml getBatchUpdate() {
        return updateXml;
    }

    @Override
    public Map<Integer, String> getPartUuidMap() {
        return partUuidMap;
    }

    @Override
    public Map<Integer, String> getItemUuidMap() {
        return itemUuidMap;
    }
}
