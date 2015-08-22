package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.req.ax.IdObject;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

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


/**
 * Abstraktní osoby.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 * @since 21.8.2015
 */
@Entity(name = "par_abstract_party")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParAbstractParty implements IdObject<Integer>, cz.tacr.elza.api.ParAbstractParty<RegRecord, ParPartySubtype> {

    @Id
    @GeneratedValue
    private Integer abstractPartyId;

    @OneToOne(fetch = FetchType.LAZY, targetEntity = RegRecord.class)
    @JoinColumn(name = "recordId", nullable = false)
    private RegRecord record;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParPartySubtype.class)
    @JoinColumn(name = "partySubtypeId", nullable = false)
    private ParPartySubtype partySubtype;


    @Override
    public Integer getAbstractPartyId() {
        return abstractPartyId;
    }

    @Override
    public void setAbstractPartyId(final Integer abstractPartyId) {
        this.abstractPartyId = abstractPartyId;
    }

    @Override
    public RegRecord getRecord() {
        return record;
    }

    @Override
    public void setRecord(final RegRecord record) {
        this.record = record;
    }

    @Override
    public ParPartySubtype getPartySubtype() {
        return partySubtype;
    }

    @Override
    public void setPartySubtype(final ParPartySubtype partySubtype) {
        this.partySubtype = partySubtype;
    }

    @Override
    @JsonIgnore
    public Integer getId() {
        return abstractPartyId;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.api.ParPartySubtype)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ParAbstractParty other = (ParAbstractParty) obj;

        return new EqualsBuilder().append(getId(), other.getId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getId()).toHashCode();
    }

}
