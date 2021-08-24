package cz.tacr.elza.service.cam;

import cz.tacr.cam.schema.cam.BatchUpdateSavedXml;
import cz.tacr.cam.schema.cam.BatchUpdateXml;

/**
 * Interface to process upload
 * 
 *
 */
public interface UploadWorker {
    public BatchUpdateXml getBatchUpdate();

    public void process(CamService camService, BatchUpdateSavedXml batchUpdateResult);
}
