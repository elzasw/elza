package cz.tacr.elza.domain;

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
 * Vazební tabulka mezi entitami {@link RegRegisterType} a {@link RulDescItemSpec}.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 * @since 21.10.2015
 */
@Entity(name = "rul_desc_item_spec_register")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulDescItemSpecRegister implements  cz.tacr.elza.api.RulDescItemSpecRegister<RegRegisterType, RulDescItemSpec> {

    @Id
    @GeneratedValue
    private Integer descItemSpecRegisterId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RegRegisterType.class)
    @JoinColumn(name = "registerTypeId", nullable = true)
    private RegRegisterType registerType;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulDescItemSpec.class)
    @JoinColumn(name = "descItemSpecId", nullable = true)
    private RulDescItemSpec descItemSpec;

    @Override
    public Integer getDescItemSpecRegisterId() {
        return descItemSpecRegisterId;
    }

    @Override
    public void setDescItemSpecRegisterId(Integer descItemSpecRegisterId) {
        this.descItemSpecRegisterId = descItemSpecRegisterId;
    }

    @Override
    public RegRegisterType getRegisterType() {
        return registerType;
    }

    @Override
    public void setRegisterType(RegRegisterType registerType) {
        this.registerType = registerType;
    }

    @Override
    public RulDescItemSpec getDescItemSpec() {
        return descItemSpec;
    }

    @Override
    public void setDescItemSpec(RulDescItemSpec descItemSpec) {
        this.descItemSpec = descItemSpec;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof RulDescItemSpecRegister)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        cz.tacr.elza.api.RulDescItemSpecRegister other = (cz.tacr.elza.api.RulDescItemSpecRegister) obj;

        return new EqualsBuilder().append(descItemSpecRegisterId, other.getDescItemSpecRegisterId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(descItemSpecRegisterId).toHashCode();
    }

    @Override
    public String toString() {
        return "RulDescItemSpecRegister pk=" + descItemSpecRegisterId;
    }
}
