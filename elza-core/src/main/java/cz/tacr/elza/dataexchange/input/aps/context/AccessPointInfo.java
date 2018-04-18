package cz.tacr.elza.dataexchange.input.aps.context;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApType;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.context.PersistMethod;

/**
 * Access point import info which primarily stores id and result of record pairing.
 */
public class AccessPointInfo extends EntityIdHolder<ApAccessPoint> {

    private final String entryId;

    private final ApType apType;

    private PersistMethod persistMethod;

    private String name;

    AccessPointInfo(String entryId, ApType apType) {
        super(ApAccessPoint.class);
        this.entryId = Validate.notNull(entryId);
        this.apType = Validate.notNull(apType);
    }

    @Override
    public Integer getEntityId() {
        return (Integer) super.getEntityId();
    }

    public String getEntryId() {
        return entryId;
    }

    public ApType getApType() {
        return apType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isIgnored() {
        Validate.notNull(persistMethod);
        return persistMethod == PersistMethod.NONE;
    }

    public PersistMethod getPersistMethod() {
        return persistMethod;
    }

    void setPersistMethod(PersistMethod persistMethod) {
        this.persistMethod = persistMethod;
    }
}
