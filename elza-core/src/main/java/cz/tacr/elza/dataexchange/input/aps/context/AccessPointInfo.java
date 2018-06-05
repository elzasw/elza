package cz.tacr.elza.dataexchange.input.aps.context;

import cz.tacr.elza.domain.ApType;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.context.PersistMethod;
import cz.tacr.elza.domain.ApAccessPoint;

/**
 * Access point import info which primarily stores id and result of record pairing.
 */
public class AccessPointInfo extends EntityIdHolder<ApAccessPoint> {

    private final String entryId;

    private final ApType apType;

    private PersistMethod persistMethod;

    private String fulltext;

    AccessPointInfo(String entryId, ApType apType) {
        super(ApAccessPoint.class);
        this.entryId = Validate.notNull(entryId);
        this.apType = Validate.notNull(apType);
    }

    public String getEntryId() {
        return entryId;
    }

    public ApType getApType() {
        return apType;
    }

    public PersistMethod getPersistMethod() {
        return persistMethod;
    }

    public void setPersistMethod(PersistMethod persistMethod) {
        this.persistMethod = persistMethod;
    }
    
    public String getFulltext() {
        return fulltext;
    }

    public void setFulltext(String fulltext) {
        this.fulltext = fulltext;
    }
}
