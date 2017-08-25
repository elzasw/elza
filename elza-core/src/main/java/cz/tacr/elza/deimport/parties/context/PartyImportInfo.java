package cz.tacr.elza.deimport.parties.context;

import java.io.Serializable;

import org.apache.commons.lang3.Validate;
import org.hibernate.CacheMode;
import org.hibernate.Session;

import cz.tacr.elza.core.data.PartyType;
import cz.tacr.elza.deimport.aps.context.RecordImportInfo;
import cz.tacr.elza.deimport.context.StatefulIdHolder;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.utils.HibernateUtils;

public class PartyImportInfo extends StatefulIdHolder {

    private final String importId;

    private final RecordImportInfo recordInfo;

    private final PartyType partyType;

    public PartyImportInfo(String importId, RecordImportInfo recordInfo, PartyType partyType) {
        super(recordInfo);
        this.importId = Validate.notNull(importId);
        this.recordInfo = Validate.notNull(recordInfo);
        this.partyType = Validate.notNull(partyType);
    }

    public String getImportId() {
        return importId;
    }

    public PartyType getPartyType() {
        return partyType;
    }

    public Integer getRecordId() {
        return recordInfo.getId();
    }

    public String getFulltext() {
        return recordInfo.getFulltext();
    }

    public void setFulltext(String fulltext) {
        recordInfo.setFulltext(fulltext);
    }

    public RegRecord getRecordRef(Session session) {
        return recordInfo.getEntityRef(session, RegRecord.class);
    }

    @Override
    public Integer getId() {
        return (Integer) super.getId();
    }

    @Override
    public void checkReferenceClass(Class<?> entityClass) {
        // any specialization of party can be used for entity reference
        if (!ParParty.class.isAssignableFrom(entityClass)) {
            throw new IllegalStateException("Class " + entityClass + " is not suitable as entity reference");
        }
    }

    /**
     * Creates updatable party reference with {@link CacheMode#NORMAL}.
     *
     * @throws IllegalStateException When holder is not initialized or supplied class is not
     *             acceptable as reference.
     * @see HibernateUtils#getReferenceWithoutCache(Session, Class, Serializable, boolean)
     */
    ParParty getUpdatablePartyRef(Session session) {
        Validate.isTrue(isInitialized(), "Holder is not initialized");
        checkReferenceClass(ParParty.class);
        return HibernateUtils.getReference(session, ParParty.class, getId(), true);
    }

    /**
     * Creates updatable record reference with {@link CacheMode#NORMAL}.
     *
     * @throws IllegalStateException When holder is not initialized or supplied class is not
     *             acceptable as reference.
     * @see HibernateUtils#getReferenceWithoutCache(Session, Class, Serializable, boolean)
     */
    RegRecord getUpdatableRecordRef(Session session) {
        Validate.isTrue(recordInfo.isInitialized(), "Holder is not initialized");
        recordInfo.checkReferenceClass(RegRecord.class);
        return HibernateUtils.getReference(session, RegRecord.class, recordInfo.getId(), true);
    }

    @Override
    protected void init(Serializable id) {
        super.init(id);
    }
}
