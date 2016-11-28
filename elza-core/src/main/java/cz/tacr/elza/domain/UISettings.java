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
 * Implementace {@link cz.tacr.elza.api.UISettings}
 *
 * @author Martin Šlapa
 * @since
 */
@Entity(name = "ui_settings")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class UISettings implements cz.tacr.elza.api.UISettings<UsrUser> {

    @Id
    @GeneratedValue
    private Integer settingsId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrUser.class)
    @JoinColumn(name = "userId", nullable = true)
    private UsrUser user;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private SettingsType settingsType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = true)
    private EntityType entityType;

    @Column(nullable = true)
    private Integer entityId;

    @Column(nullable = true)
    private String value;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId")
    private RulPackage rulPackage;

    @Override
    public Integer getSettingsId() {
        return settingsId;
    }

    @Override
    public void setSettingsId(final Integer settingsId) {
        this.settingsId = settingsId;
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
    public SettingsType getSettingsType() {
        return settingsType;
    }

    @Override
    public void setSettingsType(final SettingsType settingsType) {
        this.settingsType = settingsType;
    }

    @Override
    public EntityType getEntityType() {
        return entityType;
    }

    @Override
    public void setEntityType(final EntityType entityType) {
        this.entityType = entityType;
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    @Override
    public void setEntityId(final Integer entityId) {
        this.entityId = entityId;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(final String value) {
        this.value = value;
    }

    public RulPackage getRulPackage() {
        return rulPackage;
    }

    public void setRulPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }
}
