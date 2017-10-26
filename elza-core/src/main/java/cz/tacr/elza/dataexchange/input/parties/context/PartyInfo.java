package cz.tacr.elza.dataexchange.input.parties.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.core.data.PartyType;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointInfo;
import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.dataexchange.input.context.PersistMethod;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.utils.HibernateUtils;

public class PartyInfo extends EntityIdHolder<ParParty> {

    private final String importId;

    private final AccessPointInfo apInfo;

    private final PartyType partyType;

    public PartyInfo(String importId, AccessPointInfo apInfo, PartyType partyType) {
        super(ParParty.class);
        this.importId = Validate.notNull(importId);
        this.apInfo = Validate.notNull(apInfo);
        this.partyType = Validate.notNull(partyType);
    }

    @Override
    public Integer getEntityId() {
        return (Integer) super.getEntityId();
    }

    public String getImportId() {
        return importId;
    }

    public PartyType getPartyType() {
        return partyType;
    }

    public String getFulltext() {
        return apInfo.getFulltext();
    }

    public void setFulltext(String fulltext) {
        apInfo.setFulltext(fulltext);
    }

    public boolean isIgnored() {
        return apInfo.isIgnored();
    }

    public PersistMethod getPersistMethod() {
        return apInfo.getPersistMethod();
    }

    public Integer getAPId() {
        return apInfo.getEntityId();
    }

    public RegRecord getAPReference(Session session) {
        return apInfo.getEntityReference(session);
    }

    public RegRecord getUpdatableAPReference(Session session) {
        return HibernateUtils.getEntityReference(apInfo.getEntityId(), apInfo.getEntityClass(), session, true);
    }

    @Override
    protected boolean isReferenceInitOnDemand() {
        return true;
    }
}
