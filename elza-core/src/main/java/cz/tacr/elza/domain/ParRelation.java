package cz.tacr.elza.domain;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;


/**
 * //TODO marik missing comment
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 */
@Entity(name = "par_relation")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParRelation extends AbstractVersionableEntity implements cz.tacr.elza.api.ParRelation<ParParty, ParRelationType, ParUnitdate> {

    /**
     * Výchozí řazení objektů podle class_type ( vznik, zánik, vztah).
     */
    private static final Map<String, Integer> classTypeOrderMap = new HashMap<>();

    static {
        classTypeOrderMap.put(ParRelationType.ClassType.VZNIK.getClassType(), 1);
        classTypeOrderMap.put(ParRelationType.ClassType.ZANIK.getClassType(), 2);
        classTypeOrderMap.put(ParRelationType.ClassType.VZTAH.getClassType(), 3);
    }


    @Id
    @GeneratedValue
    private Integer relationId;

    @RestResource(exported = false)
    @OneToOne(fetch = FetchType.LAZY, targetEntity = ParParty.class)
    @JoinColumn(name = "partyId", nullable = false)
    private ParParty party;

    @RestResource(exported = false)
    @OneToOne(fetch = FetchType.LAZY, targetEntity = ParRelationType.class)
    @JoinColumn(name = "relationTypeId", nullable = false)
    private ParRelationType complementType;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ParUnitdate.class)
    @JoinColumn(name = "fromUnitdateId")
    private ParUnitdate from;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ParUnitdate.class)
    @JoinColumn(name = "toUnitdateId")
    private ParUnitdate to;

    @Column(length = StringLength.LENGTH_1000)
    private String dateNote;

    @Column(length = StringLength.LENGTH_1000)
    private String note;

    @Column
    private String source;


    @Override
    public Integer getRelationId() {
        return relationId;
    }

    @Override
    public void setRelationId(final Integer relationId) {
        this.relationId = relationId;
    }

    @Override
    public ParParty getParty() {
        return party;
    }

    @Override
    public void setParty(final ParParty party) {
        this.party = party;
    }

    @Override
    public ParRelationType getComplementType() {
        return complementType;
    }

    @Override
    public void setComplementType(final ParRelationType complementType) {
        this.complementType = complementType;
    }

    @Override
    public ParUnitdate getFrom() {
        return from;
    }

    @Override
    public void setFrom(final ParUnitdate from) {
        this.from = from;
    }

    @Override
    public ParUnitdate getTo() {
        return to;
    }

    @Override
    public void setTo(final ParUnitdate to) {
        this.to = to;
    }

    @Override
    public String getDateNote() {
        return dateNote;
    }

    @Override
    public void setDateNote(final String dateNote) {
        this.dateNote = dateNote;
    }

    @Override
    public String getNote() {
        return note;
    }

    @Override
    public void setNote(final String note) {
        this.note = note;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public void setSource(final String source) {
        this.source = source;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ParRelation)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ParRelation other = (ParRelation) obj;

        return new EqualsBuilder().append(relationId, other.getRelationId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(relationId).toHashCode();
    }

    @Override
    public String toString() {
        return "ParRelation pk=" + relationId;
    }


    /**
     * Řazení objektů.
     */
    public static class ParRelationComparator implements Comparator<ParRelation> {

        @Override
        public int compare(final ParRelation o1, final ParRelation o2) {

            Integer class1 = classTypeOrderMap.get(o1.getComplementType().getClassType());
            Integer class2 = classTypeOrderMap.get(o2.getComplementType().getClassType());
            class1 = class1 == null ? Integer.MAX_VALUE : class1;
            class2 = class2 == null ? Integer.MAX_VALUE : class2;

            int result = class1.compareTo(class2);
            if (result == 0) {
                result = o1.getRelationId().compareTo(o2.getRelationId());
            }

            return result;
        }
    }
}
