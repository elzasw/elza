package cz.tacr.elza.deimport.parties.context;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.deimport.context.EntityState;
import cz.tacr.elza.deimport.storage.EntityMetrics;
import cz.tacr.elza.deimport.storage.EntityWrapper;
import cz.tacr.elza.domain.RegRecord;

public class PartyAccessPointWrapper implements EntityWrapper, EntityMetrics {

    private final PartyImportInfo partyInfo;

    private final String name;

    private final String characteristics;

    private final String note;

    private RegRecord recordRef;

    public PartyAccessPointWrapper(PartyImportInfo partyInfo, String name, String characteristics, String note) {
        this.partyInfo = Validate.notNull(partyInfo);
        this.name = Validate.notNull(name);
        this.characteristics = characteristics;
        this.note = note;
    }

    @Override
    public EntityState getState() {
        return partyInfo.getState().equals(EntityState.IGNORE) ? EntityState.IGNORE : EntityState.UPDATE;
    }

    @Override
    public RegRecord getEntity() {
        return recordRef;
    }

    @Override
    public long getMemoryScore() {
        return 1;
    }

    @Override
    public void beforeEntityPersist(Session session) {
        recordRef = partyInfo.getUpdatableRecordRef(session);
        recordRef.setRecord(name);
        recordRef.setCharacteristics(characteristics);
        recordRef.setNote(note);
    }
}
