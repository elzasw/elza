package cz.tacr.elza.cam.v2;

import cz.tacr.cam.v2.schema.cam.BatchUpdateResultXml;
import cz.tacr.cam.v2.schema.cam.BatchUpdateXml;

/**
 * Interface to process upload
 */
public interface UploadWorker {

	public BatchUpdateXml getBatchUpdate();

	public void updateBinding(CamService camService, BatchUpdateResultXml batchUpdateResult);
}
