package cz.tacr.elza.print.party;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.print.Record;
import cz.tacr.elza.print.UnitDate;

/**
 * Helper object for party initialization
 */
public class PartyInitHelper {

    /**
     * Comparator for party names
     *
     */    
    static Comparator<PartyName> partyNameComparator = new Comparator<PartyName>() {

        public int compare(PartyName pn1, PartyName pn2) {
            if (pn1 == pn2) {
                return 0;
            }
            // Compare usage
            UnitDate validFrom1 = pn1.getValidFrom();
            UnitDate validFrom2 = pn2.getValidFrom();
            if (validFrom1 != null) {
                if (validFrom2 == null) {
                    return -1;
                }
                // both dates are valid
                int result = compare(validFrom1, validFrom2);
                if (result != 0) {
                    return result;
                }
            } else if (validFrom2 != null) {
                return 1;
            }

            // no usage or same usage -> compare texts
            String text1 = pn1.formatWithAllDetails();
            String text2 = pn2.formatWithAllDetails();
            return text1.compareTo(text2);
        }

        private int compare(UnitDate validFrom1, UnitDate validFrom2) {
            String value1 = validFrom1.getValueFrom();
            String value2 = validFrom2.getValueFrom();
            if (value1 != null) {
                if (value2 == null) {
                    return -1;
                }
                // both values exists - simply compare as strings (ISO formats)
                return value1.compareTo(value2);
            } else {
                if (value2 != null) {
                    return 1;
                }
            }
            return 0;
        }

    };

    private final Record ap;

    private PartyName preferredName;

    private final TreeSet<PartyName> names = new TreeSet<>(partyNameComparator);

    private Relation creation;

    private Relation destruction;

    private List<Relation> relations;

    private List<RelationsByType> relationsByTypeList;

    public PartyInitHelper(Record ap) {
        this.ap = Validate.notNull(ap);
    }

    public Record getAP() {
        return ap;
    }

    public PartyName getPreferredName() {
        return preferredName;
    }

    public void setPreferredName(PartyName preferredName) {
        this.preferredName = preferredName;
    }

    public Collection<PartyName> getNames() {
        return names;
    }

    public void addName(PartyName name) {
        Validate.notNull(name);
        names.add(name);
    }

    public Relation getCreation() {
        return creation;
    }

    public void setCreation(Relation creation) {
        this.creation = creation;
    }

    public Relation getDestruction() {
        return destruction;
    }

    public void setDestruction(Relation destruction) {
        this.destruction = destruction;
    }

    public List<Relation> getRelations() {
        return relations;
    }

    public List<RelationsByType> getRelationsByType() {
        return relationsByTypeList;
    }

    public void addRelation(Relation relation) {
        Validate.notNull(relation);

        if (relations == null) {
            relations = new ArrayList<>();
        }
        relations.add(relation);

        RelationsByType relByType = getRelationsByType(relation.getType());
        relByType.addRelation(relation);
    }

    private RelationsByType getRelationsByType(RelationType type) {
        if (relationsByTypeList == null) {
            relationsByTypeList = new ArrayList<>();
        } else {
            // find existing by relation type
            for (RelationsByType relByType : relationsByTypeList) {
                if (relByType.getRelationType() == type) {
                    return relByType;
                }
            }
        }
        // create new
        RelationsByType relByType = new RelationsByType(type);
        relationsByTypeList.add(relByType);
        return relByType;
    }
}
