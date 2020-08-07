package cz.tacr.elza.print.party;

import java.util.Collection;
import java.util.List;

import cz.tacr.elza.print.Record;
import cz.tacr.elza.print.RecordType;

/**
 * Abstract party
 */
public abstract class Party {

    private int partyId;

    private PartyName preferredName;

    private Collection<PartyName> names;

    private String history;

    private String sourceInformation;

    private String characteristics;

    private Record record;

    private Relation creation;

    private Relation destruction;

    private List<Relation> relations;

    private List<RelationsByType> relationsByType;


    public int getPartyId() {
        return partyId;
    }

    public String getType() {
        return null;
    }

    public String getTypeCode() {
        return null;
    }

    /*public Name getName() {
        return null;
    }*/

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

    public RecordType getPartyType() {
        return record.getType();
    }
}
