package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * Implementace {@link cz.tacr.elza.api.UsrPermission}.
 *
 * @author Martin Šlapa
 * @since 26.04.2016
 */
@Entity(name = "usr_permission")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class UsrPermission implements cz.tacr.elza.api.UsrPermission<UsrUser, UsrGroup, ArrFund, RegScope> {

    @Id
    @GeneratedValue
    private Integer permissionId;

    @Column(length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private Permission permission;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrUser.class)
    @JoinColumn(name = "userId")
    private UsrUser user;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrGroup.class)
    @JoinColumn(name = "groupId")
    private UsrGroup group;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fundId")
    private ArrFund fund;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RegScope.class)
    @JoinColumn(name = "scopeId")
    private RegScope scope;

    @Override
    public Integer getPermissionId() {
        return permissionId;
    }

    @Override
    public void setPermissionId(final Integer permissionId) {
        this.permissionId = permissionId;
    }

    @Override
    public Permission getPermission() {
        return permission;
    }

    @Override
    public void setPermission(final Permission permission) {
        this.permission = permission;
    }

    @Override
    public UsrUser getUser() {
        return user;
    }

    @Override
    public void setUser(final UsrUser user) {
        this.user = user;
    }

    @Override
    public UsrGroup getGroup() {
        return group;
    }

    @Override
    public void setGroup(final UsrGroup group) {
        this.group = group;
    }

    @Override
    public ArrFund getFund() {
        return fund;
    }

    @Override
    public void setFund(final ArrFund fund) {
        this.fund = fund;
    }

    @Override
    public RegScope getScope() {
        return scope;
    }

    @Override
    public void setScope(final RegScope scope) {
        this.scope = scope;
    }
}
