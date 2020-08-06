package cz.tacr.elza.print.party;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import cz.tacr.elza.print.UnitDate;

/**
 * Relations grouped by type
 *
 */
public class RelationsByType {

    /**
     * Comparator for relations
     *
     */
    static Comparator<Relation> relationComparator = new Comparator<Relation>() {

        @Override
        public int compare(Relation rel1, Relation rel2) {
            if (rel1 == rel2) {
                return 0;
            }
            // Compare usage
            UnitDate validFrom1 = rel1.getFrom();
            UnitDate validFrom2 = rel2.getFrom();
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
            int h1 = rel1.hashCode();
            int h2 = rel2.hashCode();
            return (h1 < h2) ? (-1) : ((h1 == h2) ? 0 : 1);
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

    private final TreeSet<Relation> relations = new TreeSet<>(relationComparator);

    private final RelationType relationType;

    public RelationsByType(RelationType relationType) {
        this.relationType = relationType;
    }

    public RelationType getRelationType() {
        return relationType;
    }

    public List<Relation> getRelations() {
        return new ArrayList<>(relations);
    }

    void addRelation(Relation relation) {
        relations.add(relation);
    }
}
