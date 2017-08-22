package cz.tacr.elza.deimport.parties.context;

import java.util.Objects;

import org.hibernate.Session;

import cz.tacr.elza.deimport.context.StatefulIdHolder.State;
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
        this.partyInfo = Objects.requireNonNull(partyInfo);
        this.name = Objects.requireNonNull(name);
        this.characteristics = characteristics;
        this.note = note;
    }

    @Override
    public boolean isCreate() {
        return false;
    }

    @Override
    public boolean isUpdate() {
        return !partyInfo.getState().equals(State.IGNORE);
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
