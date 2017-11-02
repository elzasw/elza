package cz.tacr.elza.dataexchange.input.aps.context;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.context.PersistMethod;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;

/**
 * Access point import info which primarily stores id and result of record pairing.
 */
public class AccessPointInfo extends EntityIdHolder<RegRecord> {

    private final String entryId;

    private final RegRegisterType registerType;

    private PersistMethod persistMethod;

    private String name;

    AccessPointInfo(String entryId, RegRegisterType registerType) {
        super(RegRecord.class);
        this.entryId = Validate.notNull(entryId);
        this.registerType = Validate.notNull(registerType);
    }

    @Override
    public Integer getEntityId() {
        return (Integer) super.getEntityId();
    }

    public String getEntryId() {
        return entryId;
    }

    public RegRegisterType getRegisterType() {
        return registerType;
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