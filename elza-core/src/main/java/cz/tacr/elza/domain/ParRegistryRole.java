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
public class ParRegistryRole
        implements cz.tacr.elza.api.ParRegistryRole<RegRegisterType, ParRelationRoleType> {

    @Id
    @GeneratedValue
    private Integer registryRoleId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RegRegisterType.class)
    @JoinColumn(name = "registerTypeId", nullable = false)
    private RegRegisterType registerType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParRelationRoleType.class)
    @JoinColumn(name = "roleTypeId", nullable = false)
    private ParRelationRoleType roleType;

    @Override
    public Integer getRegistryRoleId() {
        return registryRoleId;
    }

    @Override
    public void setRegistryRoleId(final Integer registryRoleId) {
        this.registryRoleId = registryRoleId;
    }

    @Override
    public RegRegisterType getRegisterType() {
        return registerType;
    }

    @Override
    public void setRegisterType(final RegRegisterType registerType) {
        this.registerType = registerType;
    }

    @Override
    public ParRelationRoleType getRoleType() {
        return roleType;
    }

    @Override
    public void setRoleType(final ParRelationRoleType roleType) {
        this.roleType = roleType;
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
