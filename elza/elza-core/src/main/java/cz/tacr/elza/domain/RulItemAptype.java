package cz.tacr.elza.domain;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;


/**
 * Vazební tabulka mezi entitami {@link ApType} a {@link RulItemSpec} nebo {@link RulItemType}.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 * @since 21.10.2015
 */
@Entity(name = "rul_item_aptype")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulItemAptype {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer itemAptypeId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApType.class)
    @JoinColumn(name = "apTypeId", nullable = false)
    private ApType apType;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulItemSpec.class)
    @JoinColumn(name = "itemSpecId", nullable = true)
    private RulItemSpec itemSpec;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulItemType.class)
    @JoinColumn(name = "itemTypeId", nullable = true)
    private RulItemType itemType;

    public Integer getItemAptypeId() {
        return itemAptypeId;
    }

    public void setItemAptypeId(final Integer itemAptypeId) {
        this.itemAptypeId = itemAptypeId;
    }

    public ApType getApType() {
        return apType;
    }

    public void setApType(final ApType apType) {
        this.apType = apType;
    }

    public RulItemSpec getItemSpec() {
        return itemSpec;
    }

    public void setItemSpec(final RulItemSpec descItemSpec) {
        this.itemSpec = descItemSpec;
    }

    public RulItemType getItemType() {
        return itemType;
    }

    public void setItemType(RulItemType itemType) {
        this.itemType = itemType;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof RulItemAptype)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        RulItemAptype other = (RulItemAptype) obj;

        return new EqualsBuilder().append(itemAptypeId, other.getItemAptypeId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(itemAptypeId).toHashCode();
    }

    @Override
    public String toString() {
        return "RulItemAptype pk=" + itemAptypeId;
    }
}
