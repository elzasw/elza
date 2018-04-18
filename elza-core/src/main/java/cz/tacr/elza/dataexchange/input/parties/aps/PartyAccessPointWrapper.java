package cz.tacr.elza.dataexchange.input.parties.aps;

import cz.tacr.elza.domain.ApAccessPoint;
import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.context.PersistMethod;
import cz.tacr.elza.dataexchange.input.parties.context.PartyInfo;
import cz.tacr.elza.dataexchange.input.storage.EntityMetrics;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper;

public class PartyAccessPointWrapper implements EntityWrapper, EntityMetrics {

    private final PartyInfo partyInfo;

    private final String name;

    private final String characteristics;

    private ApAccessPoint entity;

    PartyAccessPointWrapper(PartyInfo partyInfo, String name, String characteristics) {
        this.partyInfo = Validate.notNull(partyInfo);
        this.name = Validate.notNull(name);
        this.characteristics = characteristics;
    }

    @Override
    public PersistMethod getPersistMethod() {
        return partyInfo.isIgnored() ? PersistMethod.NONE : PersistMethod.UPDATE;
    }

    @Override
    public ApAccessPoint getEntity() {
        return entity;
    }

    @Override
    public long getMemoryScore() {
        return 1;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        Validate.isTrue(entity == null);

        entity = partyInfo.getUpdatableAPReference(session);
        //TODO [fric] namigrovat do novych wrapperu az budou hotove
//        entity.setRecord(name);
//        entity.setCharacteristics(characteristics);
    }
}
