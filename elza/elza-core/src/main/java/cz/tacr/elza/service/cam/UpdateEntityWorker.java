package cz.tacr.elza.service.cam;

import java.util.Map;

import cz.tacr.cam.schema.cam.BatchUpdateSavedXml;
import cz.tacr.cam.schema.cam.BatchUpdateXml;
import cz.tacr.elza.domain.ExtSyncsQueueItem;

public class UpdateEntityWorker implements UploadWorker {

    final private BatchUpdateXml updateXml;
    final private Map<Integer, String> itemUuidMap;
    final private Map<Integer, String> partUuidMap;
    final private Map<Integer, String> stateMap;
    final private ExtSyncsQueueItem extSyncsQueueItem;

    public UpdateEntityWorker(final ExtSyncsQueueItem extSyncsQueueItem,
                              final BatchUpdateXml updateXml,
                              final Map<Integer, String> itemUuidMap,
                              final Map<Integer, String> partUuidMap,
                              final Map<Integer, String> stateMap) {
        this.extSyncsQueueItem = extSyncsQueueItem;
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
    public void process(final CamService camService,
                        final BatchUpdateSavedXml batchUpdateResult) {
        camService.updateBinding(extSyncsQueueItem, batchUpdateResult, itemUuidMap, partUuidMap, stateMap);
    }

}
