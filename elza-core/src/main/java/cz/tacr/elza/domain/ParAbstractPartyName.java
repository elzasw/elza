package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import javax.persistence.*;


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
        implements cz.tacr.elza.api.ParAbstractPartyName<ParAbstractParty> {

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
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.api.ParAbstractPartyName)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ParAbstractPartyName other = (ParAbstractPartyName) obj;

        return new EqualsBuilder().append(abstractPartyNameId, other.getAbstractPartyNameId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(abstractPartyNameId).toHashCode();
    }

}
