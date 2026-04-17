package cz.tacr.elza.cam.v2;

import java.util.Map;

import cz.tacr.cam.v2.schema.cam.BatchUpdateResultXml;
import cz.tacr.cam.v2.schema.cam.BatchUpdateXml;
import cz.tacr.elza.domain.ExtSyncsQueueItem;

public class UpdateEntityWorker implements UploadWorker {

    final private BatchUpdateXml updateXml;
    final private Map<Integer, String> itemUuidMap;
    final private Map<Integer, String> partUuidMap;
    final private Map<Integer, String> stateMap;
    final private ExtSyncsQueueItem queueItem;

    public UpdateEntityWorker(final ExtSyncsQueueItem queueItem,
                              final BatchUpdateXml updateXml,
                              final Map<Integer, String> itemUuidMap,
                              final Map<Integer, String> partUuidMap,
                              final Map<Integer, String> stateMap) {
        this.queueItem = queueItem;
        this.updateXml = updateXml;
        this.itemUuidMap = itemUuidMap;
        this.partUuidMap = partUuidMap;
        this.stateMap = stateMap;
    }

    @Override
    public BatchUpdateXml getBatchUpdate() {
        return updateXml;
    }

    @Override
    public void updateBinding(final CamService camService,
                              final BatchUpdateResultXml batchUpdateResult) {
        camService.updateBinding(queueItem, batchUpdateResult, itemUuidMap, partUuidMap, stateMap, updateXml.getInfo());
    }
}
