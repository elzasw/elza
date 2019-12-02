package cz.tacr.elza.dataexchange.output.sections;

import javax.persistence.EntityManager;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.domain.DmsFile;
import cz.tacr.elza.service.DmsService;

public class DmsFileLoader
        extends AbstractEntityLoader<DmsFileInfoImpl> {

    final private ResourcePathResolver resourcePathResolver;

    protected DmsFileLoader(EntityManager em, int batchSize, ResourcePathResolver resourcePathResolver) {
        super(DmsFile.class, DmsFile.FIELD_FILE_ID, em, batchSize);
        this.resourcePathResolver = resourcePathResolver;
    }

    @Override
    protected DmsFileInfoImpl createResult(Object entity) {
        DmsFile dmsFile = (DmsFile) entity;

        return new DmsFileInfoImpl(dmsFile.getFileId(), dmsFile.getName(),
                dmsFile.getFileName(),
                dmsFile.getMimeType(),
                () -> DmsService.downloadFile(resourcePathResolver, dmsFile));
    }
}
