package cz.tacr.elza.print.party;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.print.Record;

/**
 * Party information
 */
public abstract class Party {

	private final int partyId;
    private final PartyName preferredName;
    private final List<PartyName> names = new ArrayList<>();
    private final String history;
    private final String sourceInformation;
    private final String characteristics;
    private final Record record;
    private final String type;
    private final String typeCode;
    private Relation creation;
    private Relation destruction;
    private final List<Relation> relations = new ArrayList<>();
    private final List<RelationsByType> relationsByTypeList = new ArrayList<>();
    

	protected Party(ParParty parParty, PartyInitHelper initHelper) {
    	partyId = parParty.getPartyId();
    	preferredName = PartyName.valueOf(parParty.getPreferredName());
    	// add names without preferred name
    	parParty.getPartyNames().forEach(parPartyName -> {
    		PartyName partyName = PartyName.valueOf(parPartyName);
    		if(!partyName.equals(preferredName)) {
    			names.add(partyName);
    		}
    	});
    	this.history = parParty.getHistory();
    	this.sourceInformation = parParty.getSourceInformation();
        this.characteristics = parParty.getCharacteristics();        
        this.type = parParty.getPartyType().getName();
        this.typeCode = parParty.getPartyType().getCode();
        
        // corresponding record
        this.record = initHelper.record;
        if(initHelper.relations!=null)
        {
        	for(Relation rel: initHelper.relations) {
        		RelationType relType = rel.getRelType();
        		RelationClassType relClassType = relType.getRelClassType();
        		switch(relClassType)
        		{
        		case B:
        			this.creation = rel;
        			break;
        		case E:
        			this.destruction = rel;
        			break;
        		case R:
        			addRelation(rel);        			
        		}
        	}
        }
	}

    
	/**
	 * Add relation to the internal structures
	 * @param rel
	 */
    private void addRelation(Relation rel) {
    	// add to the list
    	relations.add(rel);
		// add to the list by type
    	RelationType relType = rel.getRelType();
    	RelationsByType relByType = getRelationsByType(relType);
    	relByType.addRelation(rel);
	}

    /**
     * Return relation by type
     * @param relType
     * @return
     */
	private RelationsByType getRelationsByType(RelationType relType) {
		for(RelationsByType rels: relationsByTypeList) {
			if(rels.getRelationType()==relType) {
				return rels;
			}
		}
		// create new
		RelationsByType rels = new RelationsByType(relType);
		relationsByTypeList.add(rels);
		return rels;
	}

	public int getPartyId(){
    	return partyId;
    }

	/**
     * @return obsah položky record.getRecord()
     */
    public String getName() {
        return record.getRecord();
    }


    public String getCharacteristics() {
        return characteristics;
    }

    public String getHistory() {
        return history;
    }

    public List<PartyName> getNames() {
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

    public String getType() {
        return type;
    }

    public String getTypeCode() {
        return typeCode;
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
		return Collections.unmodifiableList(relationsByTypeList);
	}

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(o, this);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
    }
}
