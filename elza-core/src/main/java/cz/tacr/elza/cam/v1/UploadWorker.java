package cz.tacr.elza.cam.v1;

import cz.tacr.cam.v1.schema.cam.BatchUpdateSavedXml;
import cz.tacr.cam.v1.schema.cam.BatchUpdateXml;

/**
 * Interface to process upload
 * 
 *
 */
public interface UploadWorker {
    public BatchUpdateXml getBatchUpdate();

    public void process(CamService camService, BatchUpdateSavedXml batchUpdateResult);
}
