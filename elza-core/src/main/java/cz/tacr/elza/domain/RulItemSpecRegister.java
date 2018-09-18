package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Vazební tabulka mezi entitami {@link ApType} a {@link RulItemSpec}.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 * @since 21.10.2015
 */
@Entity(name = "rul_item_spec_register")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulItemSpecRegister {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer itemSpecRegisterId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApType.class)
    @JoinColumn(name = "apTypeId", nullable = true)
    private ApType apType;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulItemSpec.class)
    @JoinColumn(name = "itemSpecId", nullable = true)
    private RulItemSpec itemSpec;

    public Integer getItemSpecRegisterId() {
        return itemSpecRegisterId;
    }

    public void setItemSpecRegisterId(final Integer descItemSpecRegisterId) {
        this.itemSpecRegisterId = descItemSpecRegisterId;
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

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof RulItemSpecRegister)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        RulItemSpecRegister other = (RulItemSpecRegister) obj;

        return new EqualsBuilder().append(itemSpecRegisterId, other.getItemSpecRegisterId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(itemSpecRegisterId).toHashCode();
    }

    @Override
    public String toString() {
        return "RulItemSpecRegister pk=" + itemSpecRegisterId;
    }
}
