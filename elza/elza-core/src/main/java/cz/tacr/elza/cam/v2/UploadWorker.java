package cz.tacr.elza.cam.v2;

import java.util.List;
import java.util.Map;

import cz.tacr.cam.v2.schema.cam.BatchUpdateXml;

/**
 * Interface to process upload
 */
public interface UploadWorker {

	public BatchUpdateXml getBatchUpdate();

	/** ELZA {@code ApPart.partId} -> CAM UUID for parts being sent to bind in this batch. */
	public Map<Integer, String> getPartUuidMap();

	/** ELZA {@code ApItem.itemId} -> CAM UUID for items being sent to bind in this batch. */
	public Map<Integer, String> getItemUuidMap();

	/** Participants ({@code ap_binding_participant} entries) sent in this batch. */
	public List<ParticipantMapping> getParticipants();
}
