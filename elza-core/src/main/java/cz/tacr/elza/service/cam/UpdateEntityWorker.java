package cz.tacr.elza.service.cam;

import java.util.Map;

import cz.tacr.cam.schema.cam.BatchUpdateSavedXml;
import cz.tacr.cam.schema.cam.BatchUpdateXml;
import cz.tacr.elza.domain.ExtSyncsQueueItem;

public class UpdateEntityWorker implements UploadWorker {

    final private BatchUpdateXml updateXml;
    private Map<Integer, String> itemUuidMap;
    private Map<Integer, String> partUuidMap;
    private ExtSyncsQueueItem extSyncsQueueItem;

    public UpdateEntityWorker(final ExtSyncsQueueItem extSyncsQueueItem,
                              final BatchUpdateXml updateXml,
                              final Map<Integer, String> itemUuidMap,
                              final Map<Integer, String> partUuidMap) {
        this.extSyncsQueueItem = extSyncsQueueItem;
        this.updateXml = updateXml;
        this.itemUuidMap = itemUuidMap;
        this.partUuidMap = partUuidMap;
    }

    @Override
    public BatchUpdateXml getBatchUpdate() {
        return updateXml;
    }

    @Override
    public void process(final CamService camService,
                        final BatchUpdateSavedXml batchUpdateResult) {
        camService.updateBinding(extSyncsQueueItem, batchUpdateResult, itemUuidMap, partUuidMap);
    }

}
