package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.req.ax.IdObject;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


/**
 * Jméno abstraktní osoby.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 */
@Entity(name = "par_abstract_party_name")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParAbstractPartyName extends AbstractVersionableEntity
        implements IdObject<Integer>, cz.tacr.elza.api.ParAbstractPartyName<ParAbstractParty> {

    @Id
    @GeneratedValue
    private Integer abstractPartyNameId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParAbstractParty.class)
    @JoinColumn(name = "abstractPartyId", nullable = false)
    private ParAbstractParty abstractParty;

    @Column(nullable = false)
    private String name;


    @Override
    public Integer getAbstractPartyNameId() {
        return abstractPartyNameId;
    }

    @Override
    public void setAbstractPartyNameId(final Integer abstractPartyNameId) {
        this.abstractPartyNameId = abstractPartyNameId;
    }

    @Override
    public ParAbstractParty getAbstractParty() {
        return abstractParty;
    }

    @Override
    public void setAbstractParty(final ParAbstractParty abstractParty) {
        this.abstractParty = abstractParty;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    @JsonIgnore
    public Integer getId() {
        return abstractPartyNameId;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.api.ParAbstractPartyName)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ParAbstractPartyName other = (ParAbstractPartyName) obj;

        return new EqualsBuilder().append(getId(), other.getId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getId()).toHashCode();
    }

}
