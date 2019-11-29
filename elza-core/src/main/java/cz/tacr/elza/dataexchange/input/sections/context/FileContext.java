package cz.tacr.elza.dataexchange.input.sections.context;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.domain.ArrFile;

/**
 * Context of referenced file
 *
 */
public class FileContext {

    private final EntityIdHolder<ArrFile> idHolder;
    private final SectionContext sectionCtx;
    private final FileStorageDispatcher storageDispatcher;

    public FileContext(SectionContext sectionCtx,
                       EntityIdHolder<ArrFile> idHolder,
                       FileStorageDispatcher storageDispatcher) {
        this.sectionCtx = Validate.notNull(sectionCtx);
        this.idHolder = Validate.notNull(idHolder);
        this.storageDispatcher = storageDispatcher;
    }

    public EntityIdHolder<ArrFile> getIdHolder() {
        // TODO Auto-generated method stub
        return idHolder;
    }

}
