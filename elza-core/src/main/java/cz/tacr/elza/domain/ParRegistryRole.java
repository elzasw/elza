package cz.tacr.elza.domain;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Vazba mezi typem rejstříku a rolí entity.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 02.02.2016
 */
@Entity(name = "par_registry_role")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParRegistryRole {

    @Id
    @GeneratedValue
    private Integer registryRoleId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RegRegisterType.class)
    @JoinColumn(name = "registerTypeId", nullable = false)
    private RegRegisterType registerType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParRelationRoleType.class)
    @JoinColumn(name = "roleTypeId", nullable = false)
    private ParRelationRoleType roleType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    public Integer getRegistryRoleId() {
        return registryRoleId;
    }

    public void setRegistryRoleId(final Integer registryRoleId) {
        this.registryRoleId = registryRoleId;
    }

    public RegRegisterType getRegisterType() {
        return registerType;
    }

    public void setRegisterType(final RegRegisterType registerType) {
        this.registerType = registerType;
    }

    public ParRelationRoleType getRoleType() {
        return roleType;
    }

    public void setRoleType(final ParRelationRoleType roleType) {
        this.roleType = roleType;
    }

    public RulPackage getRulPackage() {
        return rulPackage;
    }

    public void setRulPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ParRegistryRole that = (ParRegistryRole) o;

        return new EqualsBuilder()
                .append(registryRoleId, that.registryRoleId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(registryRoleId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "ParRegistryRole{" +
                "registryRoleId=" + registryRoleId +
                ", registerType=" + registerType +
                ", roleType=" + roleType +
                '}';
    }
}
