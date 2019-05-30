package cz.tacr.elza.print.party;

import java.util.Collection;
import java.util.List;

import cz.tacr.elza.core.data.PartyType;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.print.Record;
import cz.tacr.elza.print.ap.Name;

/**
 * Abstract party
 */
public abstract class Party {

    private final int partyId;

    private final PartyName preferredName;

    private final Collection<PartyName> names;

    private final String history;

    private final String sourceInformation;

    private final String characteristics;

    private final Record record;

    private final Relation creation;

    private final Relation destruction;

    private final List<Relation> relations;

    private final List<RelationsByType> relationsByType;

    protected Party(ParParty parParty, PartyInitHelper initHelper) {
        this.partyId = parParty.getPartyId();
        this.preferredName = initHelper.getPreferredName();
        this.names = initHelper.getNames();
        this.history = parParty.getHistory();
        this.sourceInformation = parParty.getSourceInformation();
        this.characteristics = parParty.getCharacteristics();
        this.record = initHelper.getAP();
        this.creation = initHelper.getCreation();
        this.destruction = initHelper.getDestruction();
        this.relations = initHelper.getRelations();
        this.relationsByType = initHelper.getRelationsByType();
    }

    public int getPartyId() {
        return partyId;
    }

    public String getType() {
        return getPartyType().getName();
    }

    public String getTypeCode() {
        return getPartyType().getCode();
    }

    public Name getName() {
        return record.getPrefName();
    }

    public String getCharacteristics() {
        return characteristics;
    }

    public String getHistory() {
        return history;
    }

    public Collection<PartyName> getNames() {
        return names;
    }

    public PartyName getPreferredName() {
        return preferredName;
    }

    public Record getRecord() {
        return record;
    }

    public String getSourceInformation() {
        return sourceInformation;
    }

    public Relation getCreation() {
        return creation;
    }

    public Relation getDestruction() {
        return destruction;
    }

    public List<Relation> getRelations() {
        return relations;
    }

    public List<RelationsByType> getRelationsByType() {
        return relationsByType;
    }

    public abstract PartyType getPartyType();
}
