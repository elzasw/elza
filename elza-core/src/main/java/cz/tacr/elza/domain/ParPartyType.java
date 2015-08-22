package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.req.ax.IdObject;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;


/**
 * Číselník typů osob.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 * @since 21.8.2015
 */
@Entity(name = "par_party_type")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParPartyType implements IdObject<Integer>, cz.tacr.elza.api.ParPartyType {

    @Id
    @GeneratedValue
    private Integer partyTypeId;

    @Column(length = 15, nullable = false)
    private String code;

    @Column(length = 50, nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;


    @Override
    public Integer getPartyTypeId() {
        return partyTypeId;
    }

    @Override
    public void setPartyTypeId(final Integer partyTypeId) {
        this.partyTypeId = partyTypeId;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public void setCode(final String code) {
        this.code = code;
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
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    @JsonIgnore
    public Integer getId() {
        return partyTypeId;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ParPartyType)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ParPartyType other = (ParPartyType) obj;

        return new EqualsBuilder().append(getId(), other.getId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getId()).toHashCode();
    }

}
