package cz.tacr.elza.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Pomocná entita pro načítání práv.
 *
 * @since 02.05.2016
 */
@Entity(name = "usr_permission_view")
public class UsrPermissionView {

    public static final String FIELD_SCOPE = "scope";
    public static final String FIELD_USER = "user";

    /**
     * Identifikátor entity.
     */
    @Id
    @Column(name = "id", insertable = false, updatable = false)
    private Integer id;

    /**
     * Oprávnění.
     */
    @Column(length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private UsrPermission.Permission permission;

    /**
     * Uživatel ke kterému se oprávnění vztahuje.
     */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrUser.class)
    @JoinColumn(name = "userId")
    private UsrUser user;

    /**
     * Archivní soubor.
     */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fundId")
    private ArrFund fund;

    /**
     * Scope rejstříků a osob.
     */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApScope.class)
    @JoinColumn(name = "scopeId")
    private ApScope scope;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = WfIssueList.class)
    @JoinColumn(name = "issueListId")
    private WfIssueList issueList;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public UsrPermission.Permission getPermission() {
        return permission;
    }

    public void setPermission(final UsrPermission.Permission permission) {
        this.permission = permission;
    }

    public UsrUser getUser() {
        return user;
    }

    public void setUser(final UsrUser user) {
        this.user = user;
    }

    public ArrFund getFund() {
        return fund;
    }

    public void setFund(final ArrFund fund) {
        this.fund = fund;
    }

    public ApScope getScope() {
        return scope;
    }

    public void setScope(final ApScope scope) {
        this.scope = scope;
    }

    public WfIssueList getIssueList() {
        return issueList;
    }

    public void setIssueList(WfIssueList issueList) {
        this.issueList = issueList;
    }
}
