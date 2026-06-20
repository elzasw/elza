package cz.tacr.elza.cam.v2;

import java.util.List;
import java.util.Map;

import cz.tacr.cam.v2.schema.cam.BatchUpdateXml;

public class UpdateEntityWorker implements UploadWorker {

    final private BatchUpdateXml updateXml;
    final private Map<Integer, String> itemUuidMap;
    final private Map<Integer, String> partUuidMap;
    final private List<ParticipantMapping> participants;

    public UpdateEntityWorker(final BatchUpdateXml updateXml,
                              final Map<Integer, String> itemUuidMap,
                              final Map<Integer, String> partUuidMap,
                              final List<ParticipantMapping> participants) {
        this.updateXml = updateXml;
        this.itemUuidMap = itemUuidMap;
        this.partUuidMap = partUuidMap;
        this.participants = participants;
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

    @Override
    public List<ParticipantMapping> getParticipants() {
        return participants;
    }
}
